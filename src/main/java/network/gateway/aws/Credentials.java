package network.gateway.aws;


import application.Config;
import com.amazonaws.auth.AWSCredentials;

public class Credentials implements AWSCredentials {

    public static final Credentials PROVIDER = new Credentials();

    private Credentials() {}

    @Override
    public String getAWSAccessKeyId() {
        return Config.AWS_ACCESS_KEY;
    }

    @Override
    public String getAWSSecretKey() {
        return Config.AWS_SECRET_KEY;
    }
}
