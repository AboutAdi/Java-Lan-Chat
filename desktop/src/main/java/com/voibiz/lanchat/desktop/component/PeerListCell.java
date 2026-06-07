package com.voibiz.lanchat.desktop.component;

import com.voibiz.lanchat.core.model.Peer;
import javafx.scene.control.ListCell;

public class PeerListCell extends ListCell<Peer> {

    @Override
    protected void updateItem(Peer item, boolean empty) {
        super.updateItem(item, empty);
        
        if (empty || item == null) {
            setText(null);
            setGraphic(null);
            getStyleClass().remove("peer-cell-online");
        } else {
            String status = item.getStatus() != null ? item.getStatus().toString() : "ONLINE";
            setText(item.getDisplayName() + " (" + status + ")");
            
            if (!getStyleClass().contains("peer-cell-online") && "ONLINE".equals(status)) {
                getStyleClass().add("peer-cell-online");
            } else if (!"ONLINE".equals(status)) {
                getStyleClass().remove("peer-cell-online");
            }
        }
    }
}
