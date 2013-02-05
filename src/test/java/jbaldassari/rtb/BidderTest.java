package jbaldassari.rtb;

import java.util.Queue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.avro.ipc.CallFuture;
import org.apache.avro.ipc.Callback;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for the Bidder protocol.
 */
public class BidderTest extends AbstractBidderTest {
  private static final int NUM_REQUESTS = 10;
  
  @Test
  public void sendSynchronousBidRequest() throws Exception {
    // Initialize a bidder proxy instance, which connects to the server:
    Bidder bidder = startServerAndGetClient(new RandomBidder(SNIPPET));
    
    // Send bid requests synchronously, waiting for each to return before 
    // sending the next:
    for (int ii = 0; ii < NUM_REQUESTS; ii++) {
      BidResponse bidResponse = bidder.bid(bidRequest);
      validateBidResponse(bidRequest, bidResponse);
    }
  }
  
  @Test
  public void sendBidRequestWithCallback() throws Exception {    
    // Initialize a bidder proxy instance, which connects to the server:
    Bidder.Callback bidder = startServerAndGetClient(new RandomBidder(SNIPPET));
    
    // Send bid requests, inserting the results asynchronously into a queue:
    final Queue<BidResponse> responses = new LinkedBlockingQueue<BidResponse>();
    final CountDownLatch responsesLatch = new CountDownLatch(NUM_REQUESTS);
    for (int ii = 0; ii < NUM_REQUESTS; ii++) {
      bidder.bid(bidRequest, new Callback<BidResponse>() {
        @Override
        public void handleResult(BidResponse bidResponse) {
          responses.add(bidResponse);
          responsesLatch.countDown();
        }
        @Override
        public void handleError(Throwable t) {
          responsesLatch.countDown();
        }
      });
    }
    
    // Wait for all requests to complete:
    Assert.assertTrue(responsesLatch.await(10, TimeUnit.SECONDS));
    Assert.assertEquals(NUM_REQUESTS, responses.size());
    for (BidResponse bidResponse : responses) {
      validateBidResponse(bidRequest, bidResponse);
    }
  }
  
  @Test
  public void sendBidRequestWithFuture() throws Exception {
    // Initialize a bidder proxy instance, which connects to the server:
    Bidder.Callback bidder = startServerAndGetClient(new RandomBidder(SNIPPET));
    
    // Send bid requests, using a CallFuture to wait for each result:
    for (int ii = 0; ii < NUM_REQUESTS; ii++) {
      CallFuture<BidResponse> bidResponseFuture = new CallFuture<BidResponse>();
      bidder.bid(bidRequest, bidResponseFuture);
      BidResponse bidResponse = bidResponseFuture.get(10, TimeUnit.SECONDS);
      validateBidResponse(bidRequest, bidResponse);
    }
  }
}
