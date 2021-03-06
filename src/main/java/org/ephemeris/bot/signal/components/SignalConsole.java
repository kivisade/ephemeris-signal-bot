package org.ephemeris.bot.signal.components;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

import org.whispersystems.signalservice.api.messages.SignalServiceAttachment;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import de.thoffbauer.signal4j.listener.ConversationListener;
import de.thoffbauer.signal4j.store.User;

public class SignalConsole extends Signal {
	
	private ArrayList<ConversationListener> listeners = new ArrayList<>();
	private Scanner scanner = new Scanner(System.in);

	@Override
	public void sendMessage(String address, SignalServiceDataMessage message) throws IOException {
		ArrayList<String> addresses = new ArrayList<>();
		addresses.add(address);
		sendMessage(addresses, message);
	}

	@Override
	public void sendMessage(List<String> addresses, SignalServiceDataMessage message) throws IOException {
		String out = String.format(
				"Send message to %s:\n"
				+ "%s",
				String.join(", ", addresses),
				message.getBody().or("no body"));
		if(message.getAttachments().isPresent() && !message.getAttachments().get().isEmpty()) {
			Path attachments = Paths.get("attachments");
			if(!Files.exists(attachments)) {
				Files.createDirectory(attachments);
			}
			message.getAttachments().get().stream()
					.map(SignalServiceAttachment::asStream)
					.map(stream -> {
						Path path = attachments.resolve("bot" + System.currentTimeMillis() + "." 
													+ stream.getContentType().split("/")[1]);
						try {
							Files.copy(stream.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
						} catch (IOException e) {
							System.err.println("Could not save an attachment! " + e.getMessage() + " (" + e.getClass().getSimpleName() + ")");
							return null;
						}
						return path;
					})
					.filter(Objects::nonNull)
					.forEach(p -> System.out.println("Saved an attachment to " + p.toString()));
		}
		System.out.println(out);
	}

	@Override
	public void addConversationListener(ConversationListener listener) {
		listeners.add(listener);
	}

	@Override
	public void pull(int timeoutMillis) throws IOException {
		System.out.print("Enter sender: ");
		String sender = scanner.nextLine();
		System.out.println("Body:");
		StringBuilder body = new StringBuilder();
		String line;
		while(!(line = scanner.nextLine()).isEmpty()) {
			body.append(line).append("\n");
		}
		body = new StringBuilder(body.substring(0, body.length() - 1)); // strip off last \n
		
		SignalServiceDataMessage message = SignalServiceDataMessage.newBuilder()
				.withTimestamp(System.currentTimeMillis())
				.withBody(body.toString())
				.build();
		for(ConversationListener listener : listeners) {
			listener.onMessage(new User(sender), message, null);
		}
	}

	public String getPhoneNumber() {
		return null;
	}
}
