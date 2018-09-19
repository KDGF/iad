package com.kdg.gnome.io;

import com.kdg.gnome.util.Bytes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.security.MessageDigest;

/**
 * @author mingzhang2
 */
public class MD5HashTest {
  private static final String TEST_STRING = "hello";

  private MD5Hash md5Hash;

  @Before
  public void setup() throws Exception{
    MessageDigest digest = MessageDigest.getInstance("MD5");
    digest.update(Bytes.toBytes(TEST_STRING));
    md5Hash = new MD5Hash(digest.digest());
  }

  @After
  public void cleanup() {

  }


  @Test
  public void digestStringCase() {
    // Calculate the correct digest for the test string
    MD5Hash expectedHash = MD5Hash.digest(TEST_STRING);
    Assert.assertEquals(expectedHash, md5Hash);
    // Hashing again should give the same result
    Assert.assertEquals(expectedHash, MD5Hash.digest(TEST_STRING));
  }
}
