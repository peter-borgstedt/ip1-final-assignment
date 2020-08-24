package services;

import java.io.File;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * IP1 (IB906C), VT 2020 Internet Programming, Stationary Units
 *
 * Service uploading files o AWS Simple Storage.
 *
 * @author <a href="mailto:pebo6883@student.su.se">Peter Borgstedt</a>
 */
public class S3 {
  private static final Logger log = LogManager.getLogger(S3.class);

  private final AmazonS3 amazonS3;
  private final String bucket;
  private final  String region;

  public S3 (String bucket) {
    this.region = System.getProperty("AWS_REGION");
    var accessKeyId = System.getProperty("AWS_ACCESS_KEY_ID");
    var secretKeyId = System.getProperty("AWS_SECRET_KEY_ID");
    var credentials = new BasicAWSCredentials(accessKeyId, secretKeyId);
    
    this.bucket = bucket;
    this.amazonS3 = AmazonS3ClientBuilder
      .standard()
      .withCredentials(new AWSStaticCredentialsProvider(credentials))
      .withRegion(this.region)
      .build();
  }
  
  public String put(String path, File file) {
    try {
      // Need to set the access control list (ACL) as the bucket is partially open
      // not exposing files that should not be exposed, therefor type of access needs
      // to be set explicitly: https://stackoverflow.com/a/6524088
      this.amazonS3.putObject(new PutObjectRequest(this.bucket, path, file)
        .withCannedAcl(CannedAccessControlList.PublicRead));

    } catch (AmazonServiceException e) {
      log.error(e.getErrorMessage(), e);
    }
    return String.format("https://%s.s3-%s.amazonaws.com/%s", this.bucket, region, path);
  }
}