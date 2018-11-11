package org.ephemeris.bot.signal.components;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import de.thoffbauer.signal4j.SignalService;
import de.thoffbauer.signal4j.exceptions.NoGroupFoundException;
import de.thoffbauer.signal4j.listener.ConversationListener;
import de.thoffbauer.signal4j.listener.SecurityExceptionListener;
import de.thoffbauer.signal4j.store.Group;
import de.thoffbauer.signal4j.store.SignalStore;
import de.thoffbauer.signal4j.store.User;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.ephemeris.bot.signal.SignalBot;
import org.whispersystems.signalservice.api.messages.SignalServiceDataMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.security.Security;
import java.util.*;
import java.util.stream.Collectors;

public class SignalConnection extends Signal implements SecurityExceptionListener {

    private static final String USER_AGENT = "signal-bot";
    private static final String QR_CODE_IMAGE_PATH = "./SignalQR.png";
    private static final int QR_CODE_SIZE_PX = 500;

    private SignalService signalService;
    private Timer preKeysTimer;
    private String phoneNumber;

    public SignalConnection() throws IOException {
        Security.addProvider(new BouncyCastleProvider()); // throws 'java.security.ProviderException: Could not derive key' on Windows without this line

        signalService = new SignalService();

        if (!signalService.isRegistered()) {
            register();
        }

        preKeysTimer = new Timer(true);
        preKeysTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    signalService.checkPreKeys(80);
                } catch (IOException e) {
                    SignalBot.err("Could not update prekeys! " + e.getMessage());
                }
            }
        }, 0, 30 * 1000);
        signalService.addSecurityExceptionListener(this);

        try {
            // This is not a very elegant solution because it uses reflection to get access to the `store` private field
            // of `SignalService` class. Unfortunately, it seems like we have no other way of getting the current
            // phone number on which the bot is running, and without it we cannot distinguish true "incoming" messages
            // from "outgoing" messages (sent from other devices tied to the same phone number).
            // The best solution would probably be to just add `getPhoneNumber()` to the `SignalService` class, but
            // this class belongs to an external package which is imported via dependencies, so we would have to fork
            // that package for just one trivial change.
            Field storeField = FieldUtils.getField(SignalService.class, "store", true);
            storeField.setAccessible(true);
            SignalStore store = (SignalStore) storeField.get(signalService);
            phoneNumber = store.getPhoneNumber();
        } catch (IllegalAccessException ex) {
            // this exception will not actually be thrown because of `storeField.setAccessible(true)`,
            // but we need to catch it so that constructor function can be declared without `throws IllegalAccessException`
        }
    }

    private static String readVal(String prompt) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(prompt);
        return br.readLine();
    }

    private static String readOpt(String prompt, String[] validValues) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println(prompt);
        String input = br.readLine();

        String hint = "Unrecognized input. Please enter one of the following: " +
                Arrays.stream(validValues).map(v -> "'" + v + "'").collect(Collectors.joining(", ")) + ".";

        while (Arrays.stream(validValues).noneMatch(input::equals)) {
            System.out.println(hint);
            input = br.readLine();
        }

        return input;
    }

    private static class ConnectionSetup {
        String server, phoneNumber, deviceType;

        ConnectionSetup() {
        }

        ConnectionSetup(String url, String phoneNumber, String deviceType) {
            this.server = url;
            this.phoneNumber = phoneNumber;
            this.deviceType = deviceType;
        }

        String getUrl() {
            return server.equals("production")
                    ? "https://textsecure-service.whispersystems.org"
                    : "https://textsecure-service-staging.whispersystems.org";
        }

        String getPhoneNumber() {
            return phoneNumber;
        }

        String getDeviceType() {
            return deviceType;
        }

        boolean isPrimary() {
            return deviceType.equals("primary");
        }
    }

    private static ConnectionSetup readConnectionSetup() throws IOException {
        String server = readOpt("Url ('production' or 'staging' for whispersystems' server):",
                new String[]{"production", "staging"});

        String phoneNumber = readVal("Phone number:");

        String deviceType = readOpt("Device type ('primary' for new registration or 'secondary' for linking):",
                new String[]{"primary", "secondary"});

        return new ConnectionSetup(server, phoneNumber, deviceType);
    }

    // See https://www.callicoder.com/generate-qr-code-in-java-using-zxing/
    private static void generateQRCodeImage(String text) throws WriterException, IOException {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, QR_CODE_SIZE_PX, QR_CODE_SIZE_PX);

        Path path = FileSystems.getDefault().getPath(QR_CODE_IMAGE_PATH);
        MatrixToImageWriter.writeToPath(bitMatrix, "png", path);
    }

    private void register() throws IOException {
        ConnectionSetup setup = new ConnectionSetup();
        boolean skipConfigure = false;

        while (true) {
            try {
                if (!skipConfigure) {
                    setup = readConnectionSetup();
                }

                if (setup.isPrimary()) {
                    signalService.startConnectAsPrimary(setup.getUrl(), USER_AGENT, setup.getPhoneNumber(), false);
                    String code = readVal("Verification code:").replace("-", "");
                    signalService.finishConnectAsPrimary(code);
                } else {
                    String uuid = signalService.startConnectAsSecondary(setup.getUrl(), USER_AGENT, setup.getPhoneNumber());
                    System.out.println("Scan this uuid as a QR code, e.g. using an online qr code generator "
                            + "(the url does not contain sensitive information):");
                    System.out.println(uuid);
                    System.out.println("The QR code for the link above is also saved locally as " + QR_CODE_IMAGE_PATH);
                    generateQRCodeImage(uuid);
                    signalService.finishConnectAsSecondary(USER_AGENT, false);
                    signalService.requestSync();
                }

                break;
            } catch (Exception e) {
                System.out.println("An error has occurred: " + e.getMessage());
                String retry = readOpt("Would you like to retry? (retry/reconfigure/abort)",
                        new String[]{"retry", "reconfigure", "abort"});

                if (retry.equals("abort")) {
                    SignalBot.err("Failed to configure Signal bot for initial startup. Aborted.");
                    System.exit(1);
                } else {
                    // At this stage, the old instance of signalService is probably doomed, because either
                    // startConnectAsPrimary() or startConnectAsSecondary() was already called, and its internal
                    // property accountManager was initialized. Once it's done, there is no public way to undo it,
                    // and both startConnectAsPrimary() and startConnectAsSecondary() will refuse to work for the second time.
                    signalService = new SignalService();
                    skipConfigure = retry.equals("retry"); // if user choice was "reconfigure", skipConfigure will be false
                }
            }
        }

        SignalBot.log("Successfully connected to phone number %s as %s device.", setup.getPhoneNumber(), setup.getDeviceType());
    }

    public void sendMessage(String address, SignalServiceDataMessage message) throws IOException {
        signalService.sendMessage(address, message);
    }

    public void sendMessage(List<String> addresses, SignalServiceDataMessage message) throws IOException {
        signalService.sendMessage(addresses, message);
    }

    public void addConversationListener(ConversationListener listener) {
        signalService.addConversationListener(listener);
    }

    public void pull(int timeoutMillis) throws IOException {
        signalService.pull(timeoutMillis);
    }

    @Override
    public void onSecurityException(User user, Exception e) {
        System.err.println("Security Exception: " + e.getMessage() + " (" + e.getClass().getSimpleName() + ")");
        if (e instanceof NoGroupFoundException) {
            System.err.println(
                    "This error most probably occurs if this number was added to a group in a different registration. "
                            + "This can be fixed by leaving and re-entering the group.\n"
                            + "Therefore please enter the members of the group you just sent a message in manually (space seperated). "
                            + "If you do not know the members, please enter an empty line. The message will be considered a private message then:");
            @SuppressWarnings("resource") Scanner scanner = new Scanner(System.in);
            String[] members = scanner.nextLine().split(" ");
            if (members.length == 0) {
                System.err.println("Aborting");
                return;
            }
            Group group = new Group(((NoGroupFoundException) e).getId());
            group.setMembers(new ArrayList<>(Arrays.asList(members)));
            try {
                signalService.leaveGroup(group);
            } catch (IOException e1) {
                System.err.println("Could not leave group! " + e1.getMessage() + " (" + e1.getClass().getSimpleName() + ")");
            }
        }
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
