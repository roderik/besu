package net.consensys.pantheon.ethereum.jsonrpc.internal.methods.miner;

import static org.assertj.core.api.Assertions.assertThat;

import net.consensys.pantheon.ethereum.blockcreation.MiningCoordinator;
import net.consensys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import net.consensys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MinerStopTest {

  private MinerStop method;

  @Mock private MiningCoordinator miningCoordinator;

  @Before
  public void before() {
    method = new MinerStop(miningCoordinator);
  }

  @Test
  public void shouldReturnCorrectMethodName() {
    assertThat(method.getName()).isEqualTo("miner_stop");
  }

  @Test
  public void shouldReturnTrueWhenMiningStopsSuccessfully() {
    final JsonRpcRequest request = minerStop();
    final JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, true);

    final JsonRpcResponse actualResponse = method.response(request);

    assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse);
  }

  private JsonRpcRequest minerStop() {
    return new JsonRpcRequest("2.0", "miner_stop", null);
  }
}
