package bankal_deir.com;

import android.os.AsyncTask;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SendMail extends AsyncTask<Void, Void, Void> {
    private String email;
    private String password;
    private String to;
    private String subject;
    private String body;

    public SendMail(String email, String password, String to, String subject, String body) {
        this.email = email;
        this.password = password;
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    @Override
    protected Void doInBackground(Void... params) {
        try {
            Properties props = new Properties();
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "465");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.socketFactory.port", "465");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.ssl.protocols", "TLSv1.2"); // ✅ هذا هو السطر المهم

            Session session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(email, password);
                }
            });

            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(email));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setText(body);

            Transport.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
