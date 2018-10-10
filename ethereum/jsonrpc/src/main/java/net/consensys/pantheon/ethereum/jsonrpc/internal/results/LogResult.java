package net.consensys.pantheon.ethereum.jsonrpc.internal.results;

import net.consensys.pantheon.ethereum.core.LogTopic;
import net.consensys.pantheon.ethereum.jsonrpc.internal.queries.LogWithMetadata;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/** A single log result. */
@JsonPropertyOrder({
  "logIndex",
  "removed",
  "blockNumber",
  "blockHash",
  "transactionHash",
  "transactionIndex",
  "address",
  "data",
  "topics"
})
public class LogResult implements JsonRpcResult {

  private final String logIndex;
  private final String blockNumber;
  private final String blockHash;
  private final String transactionHash;
  private final String transactionIndex;
  private final String address;
  private final String data;
  private final List<String> topics;
  private final boolean removed;

  public LogResult(final LogWithMetadata logWithMetadata) {
    this.logIndex = Quantity.create(logWithMetadata.getLogIndex());
    this.blockNumber = Quantity.create(logWithMetadata.getBlockNumber());
    this.blockHash = logWithMetadata.getBlockHash().toString();
    this.transactionHash = logWithMetadata.getTransactionHash().toString();
    this.transactionIndex = Quantity.create(logWithMetadata.getTransactionIndex());
    this.address = logWithMetadata.getAddress().toString();
    this.data = logWithMetadata.getData().toString();
    this.topics = new ArrayList<>(logWithMetadata.getTopics().size());
    this.removed = logWithMetadata.isRemoved();

    for (final LogTopic topic : logWithMetadata.getTopics()) {
      topics.add(topic.toString());
    }
  }

  @JsonGetter(value = "logIndex")
  public String getLogIndex() {
    return logIndex;
  }

  @JsonGetter(value = "blockNumber")
  public String getBlockNumber() {
    return blockNumber;
  }

  @JsonGetter(value = "blockHash")
  public String getBlockHash() {
    return blockHash;
  }

  @JsonGetter(value = "transactionHash")
  public String getTransactionHash() {
    return transactionHash;
  }

  @JsonGetter(value = "transactionIndex")
  public String getTransactionIndex() {
    return transactionIndex;
  }

  @JsonGetter(value = "address")
  public String getAddress() {
    return address;
  }

  @JsonGetter(value = "data")
  public String getData() {
    return data;
  }

  @JsonGetter(value = "topics")
  public List<String> getTopics() {
    return topics;
  }

  @JsonGetter(value = "removed")
  public boolean isRemoved() {
    return removed;
  }
}
