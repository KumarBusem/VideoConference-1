/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package video;

import java.util.List;
import org.gstreamer.Bin;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.GhostPad;
import org.gstreamer.Pad;
import org.gstreamer.PadLinkReturn;
import org.gstreamer.State;
import org.gstreamer.elements.Tee;
import org.gstreamer.elements.good.RTPBin;
import util.Util;

/**
 *
 * @author chandra
 */
public class VideoSenderBin extends Bin{
    private Pad sink;
    private VideoRtpEncodeBin encoder;
    private Element udpSink;
    private RTPBin rtpBin;
    
    public VideoSenderBin(String name, String ip, int port, boolean multicast){
        super(name);   
        encoder = new VideoRtpEncodeBin();
        encoder.syncStateWithParent();
        rtpBin = new RTPBin((String) null);
        // asking this put the gstrtpbin plugin in sender mode
        Pad rtpSink0 = rtpBin.getRequestPad("send_rtp_sink_0");

        udpSink = ElementFactory.make("udpsink", null);
        udpSink.set("host", ip);
        udpSink.set("port", port);
        if (multicast) {
                // make OS automatically join multicast group
                udpSink.set("auto-multicast", true);
        }
        udpSink.set("async", false);

        // ############## ADD THEM TO PIPELINE ####################
        addMany(encoder, rtpBin, udpSink);

        // ###################### LINK THEM ##########################
        sink = new GhostPad("sink", encoder.getStaticPad("sink"));
        sink.setActive(true);
        addPad(sink);

        Util.doOrDie(
                        "encoder-rtpBin",
                        encoder.getStaticPad("src").link(rtpSink0)
                                        .equals(PadLinkReturn.OK));
        Util.doOrDie(
                        "rtpbin-udpSink",
                        rtpBin.getStaticPad("send_rtp_src_0")
                                        .link(udpSink.getStaticPad("sink"))
                                        .equals(PadLinkReturn.OK));
    
    }
    
    private Element getElementByNameStartingWith(List<Element> elts,
    String start) {
        Element ret = null;

        for (Element elt : elts) {
                if (elt.getName().startsWith(start)) {
                        return elt;
                }
        }

        return ret;
    }
    
    public Long getSSRC() {
        /*
         * we dig to find it in sink pad's caps by parsing these as a string,
         * didn't find any better way
         */
        String caps = getElementByNameStartingWith(rtpBin.getElements(),
                        "rtpsession").getSinkPads().get(0).getCaps().toString();
        int ssrcBegin = caps.indexOf("ssrc=(uint)") + 11;
        int ssrcEnd = caps.indexOf(";", ssrcBegin);
        return new Long(caps.substring(ssrcBegin, ssrcEnd));
    }
    
    
    public void getOut() {
        // clean request pad from adder
        Pad upstreamPeer = sink.getPeer();
        Tee teeUpstream = ((Tee) sink.getPeer().getParent());
        Bin parentBin = ((Bin) this.getParent());

        upstreamPeer.setBlocked(true);

        this.setState(State.NULL);

        ((Bin) this.getParent()).remove(this);

        /*
         * if upstream tee has no src anymore, the pipeline will push in the
         * void and crash, thus we avoid it by stopping the whole bin
         */
        if (teeUpstream.getSrcPads().size() == 1) {
                parentBin.setState(State.NULL);
        }
        teeUpstream.releaseRequestPad(upstreamPeer);
    }
    
    
}
