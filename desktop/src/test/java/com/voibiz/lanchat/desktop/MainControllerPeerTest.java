package com.voibiz.lanchat.desktop;

import com.voibiz.lanchat.core.model.Peer;
import com.voibiz.lanchat.core.model.User;
import com.voibiz.lanchat.desktop.controller.MainController;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import javafx.stage.Stage;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(ApplicationExtension.class)
public class MainControllerPeerTest {

    private MainController controller;

    @Start
    public void start(Stage stage) throws Exception {
        // We can't easily start MainController without FXML, so we just test the ListView behavior.
    }

    @Test
    public void testListViewPeerEquality() throws Exception {
        ListView<Peer> list = new ListView<>();
        Peer p1 = new Peer("user1", "Peer 1", "127.0.0.1", 5000, Peer.PeerStatus.ONLINE);
        
        Platform.runLater(() -> {
            list.getItems().add(p1);
        });
        Thread.sleep(200);
        
        Peer p2 = new Peer("user1", "Peer 1 Updated", "127.0.0.1", 5000, Peer.PeerStatus.ONLINE);
        
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            int index = list.getItems().indexOf(p2);
            if (index >= 0) {
                list.getItems().set(index, p2);
            } else {
                list.getItems().add(p2);
            }
            latch.countDown();
        });
        
        latch.await(2, TimeUnit.SECONDS);
        
        assertEquals(1, list.getItems().size());
        assertEquals("Peer 1 Updated", list.getItems().get(0).getDisplayName());
    }
}
