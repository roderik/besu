package net.consensys.pantheon.consensus.ibft.blockcreation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import net.consensys.pantheon.consensus.common.VoteTally;
import net.consensys.pantheon.consensus.ibft.ConsensusRoundIdentifier;
import net.consensys.pantheon.consensus.ibft.IbftBlockHashing;
import net.consensys.pantheon.consensus.ibft.IbftExtraData;
import net.consensys.pantheon.crypto.SECP256K1;
import net.consensys.pantheon.crypto.SECP256K1.KeyPair;
import net.consensys.pantheon.crypto.SECP256K1.Signature;
import net.consensys.pantheon.ethereum.chain.Blockchain;
import net.consensys.pantheon.ethereum.chain.MutableBlockchain;
import net.consensys.pantheon.ethereum.core.Address;
import net.consensys.pantheon.ethereum.core.AddressHelpers;
import net.consensys.pantheon.ethereum.core.BlockHeader;
import net.consensys.pantheon.ethereum.core.BlockHeaderTestFixture;
import net.consensys.pantheon.ethereum.core.Hash;
import net.consensys.pantheon.util.bytes.BytesValue;

import java.util.LinkedList;
import java.util.Optional;

import com.google.common.collect.Lists;
import org.junit.Test;

public class ProposerSelectorTest {

  private Blockchain createMockedBlockChainWithHeadOf(
      final long blockNumber, final KeyPair nodeKeys) {

    final IbftExtraData unsignedExtraData =
        new IbftExtraData(
            BytesValue.wrap(new byte[32]),
            Lists.newArrayList(),
            // seals are not required for this test.
            null, // No proposer seal till after block exists
            Lists.newArrayList()); // Actual content of extradata is irrelevant.

    final BlockHeaderTestFixture headerBuilderFixture = new BlockHeaderTestFixture();
    headerBuilderFixture.number(blockNumber).extraData(unsignedExtraData.encode());

    final Hash signingHash =
        IbftBlockHashing.calculateDataHashForProposerSeal(
            headerBuilderFixture.buildHeader(), unsignedExtraData);

    final Signature proposerSignature = SECP256K1.sign(signingHash, nodeKeys);

    // Duplicate the original extraData, but include the proposerSeal
    final IbftExtraData signedExtraData =
        new IbftExtraData(
            unsignedExtraData.getVanityData(),
            unsignedExtraData.getSeals(),
            proposerSignature,
            unsignedExtraData.getValidators());

    final BlockHeader prevBlockHeader =
        headerBuilderFixture.extraData(signedExtraData.encode()).buildHeader();

    // Construct a block chain and world state
    final MutableBlockchain blockchain = mock(MutableBlockchain.class);
    when(blockchain.getBlockHeader(anyLong())).thenReturn(Optional.of(prevBlockHeader));

    return blockchain;
  }

  /**
   * This creates a list of validators, with the a number of validators above and below the local
   * address. The returned list is sorted.
   *
   * @param localAddr The address of the node which signed the parent block
   * @param countLower The number of validators which have a higher address than localAddr
   * @param countHigher The number of validators which have a lower address than localAddr
   * @return A sorted list of validators which matches parameters (including the localAddr).
   */
  private LinkedList<Address> createValidatorList(
      final Address localAddr, final int countLower, final int countHigher) {
    final LinkedList<Address> result = Lists.newLinkedList();

    // Note: Order of this list is irrelevant, is sorted by value later.
    result.add(localAddr);

    for (int i = 0; i < countLower; i++) {
      result.add(AddressHelpers.calculateAddressWithRespectTo(localAddr, i - countLower));
    }

    for (int i = 0; i < countHigher; i++) {
      result.add(AddressHelpers.calculateAddressWithRespectTo(localAddr, i + 1));
    }

    result.sort(null);
    return result;
  }

  @Test
  public void roundRobinChangesProposerOnRoundZeroOfNextBlock() {
    final long PREV_BLOCK_NUMBER = 2;
    final KeyPair prevProposerKeys = KeyPair.generate();
    final Address localAddr =
        Address.extract(Hash.hash(prevProposerKeys.getPublicKey().getEncodedBytes()));

    final Blockchain blockchain =
        createMockedBlockChainWithHeadOf(PREV_BLOCK_NUMBER, prevProposerKeys);

    final LinkedList<Address> validatorList = createValidatorList(localAddr, 0, 4);
    final VoteTally voteTally = new VoteTally(validatorList);

    final ProposerSelector uut = new ProposerSelector(blockchain, voteTally, true);

    final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 0);

    final Address nextProposer = uut.selectProposerForRound(roundId);

    assertThat(nextProposer).isEqualTo(validatorList.get(1));
  }

  @Test
  public void lastValidatorInListValidatedPreviousBlockSoFirstIsNextProposer() {
    final long PREV_BLOCK_NUMBER = 2;
    final KeyPair prevProposerKeys = KeyPair.generate();

    final Blockchain blockchain =
        createMockedBlockChainWithHeadOf(PREV_BLOCK_NUMBER, prevProposerKeys);

    final Address localAddr =
        Address.extract(Hash.hash(prevProposerKeys.getPublicKey().getEncodedBytes()));

    final LinkedList<Address> validatorList = createValidatorList(localAddr, 4, 0);
    final VoteTally voteTally = new VoteTally(validatorList);

    final ProposerSelector uut = new ProposerSelector(blockchain, voteTally, true);

    final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 0);

    final Address nextProposer = uut.selectProposerForRound(roundId);

    assertThat(nextProposer).isEqualTo(validatorList.get(0));
  }

  @Test
  public void stickyProposerDoesNotChangeOnRoundZeroOfNextBlock() {
    final long PREV_BLOCK_NUMBER = 2;
    final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 0);

    final KeyPair prevProposerKeys = KeyPair.generate();
    final Blockchain blockchain =
        createMockedBlockChainWithHeadOf(PREV_BLOCK_NUMBER, prevProposerKeys);

    final Address localAddr =
        Address.extract(Hash.hash(prevProposerKeys.getPublicKey().getEncodedBytes()));
    final LinkedList<Address> validatorList = createValidatorList(localAddr, 4, 0);
    final VoteTally voteTally = new VoteTally(validatorList);

    final ProposerSelector uut = new ProposerSelector(blockchain, voteTally, false);
    final Address nextProposer = uut.selectProposerForRound(roundId);

    assertThat(nextProposer).isEqualTo(localAddr);
  }

  @Test
  public void stickyProposerChangesOnSubsequentRoundsAtSameBlockHeight() {
    final long PREV_BLOCK_NUMBER = 2;
    ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 0);

    final KeyPair prevProposerKeys = KeyPair.generate();
    final Blockchain blockchain =
        createMockedBlockChainWithHeadOf(PREV_BLOCK_NUMBER, prevProposerKeys);

    final Address localAddr =
        Address.extract(Hash.hash(prevProposerKeys.getPublicKey().getEncodedBytes()));
    final LinkedList<Address> validatorList = createValidatorList(localAddr, 4, 0);
    final VoteTally voteTally = new VoteTally(validatorList);

    final ProposerSelector uut = new ProposerSelector(blockchain, voteTally, false);
    assertThat(uut.selectProposerForRound(roundId)).isEqualTo(localAddr);

    roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 1);
    assertThat(uut.selectProposerForRound(roundId)).isEqualTo(validatorList.get(0));

    roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 2);
    assertThat(uut.selectProposerForRound(roundId)).isEqualTo(validatorList.get(1));
  }

  @Test
  public void whenProposerSelfRemovesSelectsNextProposerInLineEvenWhenSticky() {
    final long PREV_BLOCK_NUMBER = 2;
    final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 0);

    final KeyPair prevProposerKeys = KeyPair.generate();
    final Blockchain blockchain =
        createMockedBlockChainWithHeadOf(PREV_BLOCK_NUMBER, prevProposerKeys);

    final Address localAddr =
        Address.extract(Hash.hash(prevProposerKeys.getPublicKey().getEncodedBytes()));

    // LocalAddr will be in index 2 - the next proposer will also be in 2 (as prev proposer is
    // removed)
    final LinkedList<Address> validatorList = createValidatorList(localAddr, 2, 2);
    validatorList.remove(localAddr);

    // Note the signer of the Previous block was not included.
    final VoteTally voteTally = new VoteTally(validatorList);

    final ProposerSelector uut = new ProposerSelector(blockchain, voteTally, false);

    assertThat(uut.selectProposerForRound(roundId)).isEqualTo(validatorList.get(2));
  }

  @Test
  public void whenProposerSelfRemovesSelectsNextProposerInLineEvenWhenRoundRobin() {
    final long PREV_BLOCK_NUMBER = 2;
    final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 0);

    final KeyPair prevProposerKeys = KeyPair.generate();
    final Blockchain blockchain =
        createMockedBlockChainWithHeadOf(PREV_BLOCK_NUMBER, prevProposerKeys);

    final Address localAddr =
        Address.extract(Hash.hash(prevProposerKeys.getPublicKey().getEncodedBytes()));

    // LocalAddr will be in index 2 - the next proposer will also be in 2 (as prev proposer is
    // removed)
    final LinkedList<Address> validatorList = createValidatorList(localAddr, 2, 2);
    validatorList.remove(localAddr);

    // Note the signer of the Previous block was not included.
    final VoteTally voteTally = new VoteTally(validatorList);

    final ProposerSelector uut = new ProposerSelector(blockchain, voteTally, true);

    assertThat(uut.selectProposerForRound(roundId)).isEqualTo(validatorList.get(2));
  }

  @Test
  public void proposerSelfRemovesAndHasHighestAddressNewProposerIsFirstInList() {
    final long PREV_BLOCK_NUMBER = 2;
    final ConsensusRoundIdentifier roundId = new ConsensusRoundIdentifier(PREV_BLOCK_NUMBER + 1, 0);

    final KeyPair prevProposerKeys = KeyPair.generate();
    final Blockchain blockchain =
        createMockedBlockChainWithHeadOf(PREV_BLOCK_NUMBER, prevProposerKeys);

    final Address localAddr =
        Address.extract(Hash.hash(prevProposerKeys.getPublicKey().getEncodedBytes()));

    // LocalAddr will be in index 2 - the next proposer will also be in 2 (as prev proposer is
    // removed)
    final LinkedList<Address> validatorList = createValidatorList(localAddr, 4, 0);
    validatorList.remove(localAddr);

    // Note the signer of the Previous block was not included.
    final VoteTally voteTally = new VoteTally(validatorList);

    final ProposerSelector uut = new ProposerSelector(blockchain, voteTally, false);

    assertThat(uut.selectProposerForRound(roundId)).isEqualTo(validatorList.get(0));
  }
}
