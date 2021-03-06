package com.googlecode.gtalksms.xmpp;

import java.util.List;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.jxmpp.util.XmppStringUtils;

import android.content.Context;

import com.googlecode.gtalksms.tools.Log;
import com.googlecode.gtalksms.tools.Tools;

public class XmppOfflineMessages {

	public static void handleOfflineMessages(XMPPConnection connection, String[] notifiedAddresses, Context ctx)
	        throws Exception {
		Log.i("Begin retrieval of offline messages from server");
		OfflineMessageManager offlineMessageManager = new OfflineMessageManager(connection);

		if (!offlineMessageManager.supportsFlexibleRetrieval()) {
            Log.d("Offline messages not supported");
            return;
        }

		if (offlineMessageManager.getMessageCount() == 0) {
			Log.d("No offline messages found on server");
		} else {
            List<Message> msgs = offlineMessageManager.getMessages();
            for (Message msg : msgs) {
                String fullJid = msg.getFrom();
                String bareJid = XmppStringUtils.parseBareJid(fullJid);
                String messageBody = msg.getBody();
                if (messageBody != null) {
                    Log.d("Retrieved offline message from " + fullJid + " with content: " + messageBody.substring(0, Math.min(40, messageBody.length())));
                    for (String notifiedAddress : notifiedAddresses) {
                        if (bareJid.equals(notifiedAddress)) {
                            Tools.startSvcXMPPMsg(ctx, messageBody, fullJid);
                        }
                    }
                }
            }
            try {
                offlineMessageManager.deleteMessages();
            } catch (Exception e) {
                Log.e("Failed to delete offline messages from server");
            }
        }
		Log.i("End of retrieval of offline messages from server");
	}
}
