/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import senderreceiverpipe.ReceiverPipeline;
import senderreceiverpipe.SenderPipeline;
import message.Call;
import message.Message;
import message.MessageType;
import message.User;
import org.gstreamer.Element;
import org.gstreamer.ElementFactory;
import org.gstreamer.Gst;


/**
 *
 * @author chandra
 */
public class VideoConference {

    
    private GUI gui;
    private GUIConferenceRoom guiCR;
    private User myUser = null;
    private User destinationUser = null;
    private int myPositionInCR = -1;
    
    /** GStreamer pipeline to receive from rooms and contact */
    private ReceiverPipeline receiverPipeline;
    /** GStreamer pipeline to send to rooms and contact */
    private SenderPipeline senderPipeline;
    
    private ServerChannel serverChannel;
    
    private List<User>usersListLocal;
    
    private List<User>roomParticipantsLocal;
    
    public VideoConference(){
       
        //GStreamer inits
        Gst.init("AudioVideoConferencing", new String[] { "--gst-debug-level=2",
                        "--gst-debug-no-color" });
        
        
        usersListLocal = new ArrayList<>();
        roomParticipantsLocal = new ArrayList<>();
        
        
    }
    
    public void leaveRoom(int roomId) {
        /*
        getControl().send(new Leave(roomId).toString());
        for (Room room : allRooms) {
                if (room.getId() == roomId) {
                        allRooms.get(allRooms.indexOf(room)).getAudience()
                                        .remove(username);
                }
        }*/
        
        //senderAudio.stopStreamingToRoom(roomId);
        //receiverAudio.stopRoomReceiving(roomId);
        senderPipeline.stopStreamingToRoom(roomId);
        receiverPipeline.stopRoomReceiving(roomId);
    }
    
    public void askToCall(String contact) {
        if (contact.endsWith("(Disconnected)")) {
                contact = contact.substring(0, contact.length() - 15);
        }
        // open a local port for our (maybe) future conversation
        //int portVideo = this.receiverVideo.receiveFromUnicast(myRtpBin);
        //int portVideo = this.receiverAudio.receiveFromUnicast();
        //send(new Call(username, contact, "0", port));
        // remember name of my friend I'm talking with
        
        //int portAudio = this.receiverAudio.receiveFromUnicast();
        //int portAudio = this.receiverAudio.receiveFromUnicast(myRtpBin);
        
       // this.friend = contact;
    }

    public void call(String ipReceiver, int port) {
        //senderVideo.streamTo(ipReceiver, port);
        //senderAudio.streamTo(ipReceiver, port);
        //gui.getCallBtn().setVisible(false);
        //gui.getHangUpBtn().setVisible(true);
    }

    /**
    * hang up. Stop the pipelines and send a message to the server for telling
    * the other client the call is finished
    */
    public void askToStopCall() {
           stopCall();

           //send(new StopCall(friend));
    }
    
    /**
    * Stop streaming from/to friend and update UI buttons.
    */
    public void stopCall() {
           // stop streaming from friend
           //receiverVideo.stopUnicastReceiving();
           // stop streaming to friend
           //senderVideo.stopStreamingToUnicast();

           receiverPipeline.stopUnicastReceiving();
           // stop streaming to friend
           senderPipeline.stopStreamingToUnicast();
           
           //gui.getCallBtn().setVisible(true);
           //gui.getHangUpBtn().setVisible(false);
    }
    
    public void answerCall(String answer, Call call) {
        int portVideo = -1;
        int portAudio = -1;

        if (answer.equals("yes")) {
                //portVideo = receiverVideo.receiveFromUnicast(myRtpBin);
                //portVideo = receiverVideo.receiveFromUnicast();
                
                //portAudio = receiverAudio.receiveFromUnicast();
               // portAudio = receiverAudio.receiveFromUnicast();
               // this.friend = call.getSender();
                
                //gui.getCallBtn().setVisible(false);
                //gui.getHangUpBtn().setVisible(true);
        }

        //send(new AnswerCall(port, call.getSender(), call.getReceiver(), answer,
                  //          "0"));
    }

    
    public SenderPipeline getSenderPipeline(){
        return this.senderPipeline;
    }
    
    public ReceiverPipeline getReceiverPipeline(){
        return this.receiverPipeline;
    }
    
    public void setGUI(GUI gui){
        this.gui = gui;
    }

    public void setGUICR(GUIConferenceRoom guiCR){
        this.guiCR = guiCR;
    }
    
    public void signin(String userName) {
        myUser = new User(userName, "");
        serverChannel.send(new Message(MessageType.REGISTERING, userName));
    }

    void registered() {
        //hide the welcome gui
        System.out.println("User registerd");
        gui.getWelcomePanel().setVisible(false);
        //show the communication gui
        gui.getMainPanel().setVisible(true);
        
        //fetch userslist
        //serverChannel.send(new Message(MessageType.FETCHUSERS));
        
    }

    void updateUsersList(List<User> list) {
        usersListLocal = list;
         if (usersListLocal!=null){
            String[]usersArray = new String[usersListLocal.size()];
            for (int i=0;i<usersArray.length;i++){
                usersArray[i]=usersListLocal.get(i).getUserName();
                System.out.println(usersArray[i]);
            }
            gui.getUsersListList().setModel(new javax.swing.DefaultComboBoxModel(usersArray));
        }
        
    }
    
    public void init(){
        //communication with server
        serverChannel = new ServerChannel(this);
        //open communication channel to the server
        new Thread(serverChannel).start();
        
        //initialize receiver and sender pipeline
        receiverPipeline = new ReceiverPipeline(this);
        senderPipeline = new SenderPipeline(this);
        
    }

    void privateCall(int index) {
        if (destinationUser == null){
            User theUser = usersListLocal.get(index);
            if (!theUser.getUserName().equalsIgnoreCase(myUser.getUserName())){
                destinationUser = theUser;

                //ask server to tell the receiver to prepare
                serverChannel.send(new Message(MessageType.CALL_REQUEST, theUser) );

                //get ready for listening
                getReceiverPipeline().receiveFromUnicast(theUser.getIpAddress());
                System.out.println("Requesting private call to "+theUser.getIpAddress());

                gui.getButtonCall().setEnabled(false);
                gui.getButtonHangUp().setEnabled(true);
            }
        }
    }

    void acceptCall(User senderUser) {
        if (destinationUser == null){
            destinationUser = senderUser;
            //get ready for listening from the originator
            getReceiverPipeline().receiveFromUnicast(senderUser.getIpAddress());
            System.out.println("accepting call from "+senderUser.getIpAddress());

            //tell the sender that his/her call is accepted
            serverChannel.send(new Message(MessageType.CALL_ACCEPTED, senderUser));

            //start sending to the originator
            senderPipeline.streamTo(senderUser.getIpAddress());
            gui.getButtonCall().setEnabled(false);
            gui.getButtonHangUp().setEnabled(true);
        }
        
    }

    void callAccepted(User destUser) {
        senderPipeline.streamTo(destUser.getIpAddress());
        System.out.println("call accepted by "+destUser.getIpAddress());
    }
    
    public GUI getGUI(){
        return gui;
    }
    
    public GUIConferenceRoom getGUICR(){
        return guiCR;
    }
    
    public int getMyPositionInCR(){
        return myPositionInCR;
    }

    void endPrivateCall() {
        if (destinationUser!=null){
            
            gui.getButtonCall().setEnabled(true);
            gui.getButtonHangUp().setEnabled(false);
            
            serverChannel.send(new Message(MessageType.BYE, destinationUser));
            
            //stop receiviing
            receiverPipeline.stopUnicastReceiving();
            
            //stop sending
            senderPipeline.stopStreamingToUnicast();
            
            
        }
        destinationUser=null;
    }

    void joinRoom() {
        serverChannel.send(new Message(MessageType.JOIN_ROOM_REQUEST));
        
        System.out.println("start receiving");
        receiverPipeline.receiveFromRoom(99999, myUser);
        
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            Logger.getLogger(VideoConference.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        //start receiving from room
        System.out.println("start sending");
        long mySSRC = senderPipeline.streamToRoom(myUser);
        
        guiCR.getButtonQuitRoom().setEnabled(true);
        guiCR.getButtonJoinRoom().setEnabled(false);
        
    }

    void showGUIConferenceRoom() {
        guiCR.setVisible(true);
        gui.setVisible(false);
    }

    void showMainGUI() {
        guiCR.setVisible(false);
        gui.setVisible(true);
    }

    void refreshUsersList() {
        serverChannel.send(new Message(MessageType.FETCHUSERS));
    }

    void updateRoomParticipantsListLocal(List<User> roomPart) {
        roomParticipantsLocal = roomPart;
         if (roomParticipantsLocal!=null){
            String[]roomPartArray = new String[roomParticipantsLocal.size()];
            for (int i=0;i<roomPartArray.length;i++){
                roomPartArray[i]=roomParticipantsLocal.get(i).getUserName();
                System.out.println(roomPartArray[i]);
                if (roomPartArray[i].equalsIgnoreCase(myUser.getUserName())){
                    myPositionInCR = i;
                }
            }
            //gui.getUsersListList().setModel(new javax.swing.DefaultComboBoxModel(usersArray));
            guiCR.getListRoomParticipantsList().setModel(new javax.swing.DefaultComboBoxModel(roomPartArray));
         }
    }

    void quitRoom() {
        
        //notify the server
        serverChannel.send(new Message(MessageType.QUIT_ROOM));
        
        senderPipeline.stopStreamingToRoom();
        receiverPipeline.stopRoomReceiving();
        
        guiCR.getButtonJoinRoom().setEnabled(true);
        guiCR.getButtonQuitRoom().setEnabled(false);
    }
    
}
