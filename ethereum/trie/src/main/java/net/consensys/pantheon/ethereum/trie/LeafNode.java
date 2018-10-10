package net.consensys.pantheon.ethereum.trie;

import static net.consensys.pantheon.crypto.Hash.keccak256;

import net.consensys.pantheon.ethereum.rlp.BytesValueRLPOutput;
import net.consensys.pantheon.ethereum.rlp.RLP;
import net.consensys.pantheon.util.bytes.Bytes32;
import net.consensys.pantheon.util.bytes.BytesValue;

import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Function;

class LeafNode<V> implements Node<V> {
  private final BytesValue path;
  private final V value;
  private final NodeFactory<V> nodeFactory;
  private final Function<V, BytesValue> valueSerializer;
  private WeakReference<BytesValue> rlp;
  private SoftReference<Bytes32> hash;
  private boolean dirty = false;

  LeafNode(
      final BytesValue path,
      final V value,
      final NodeFactory<V> nodeFactory,
      final Function<V, BytesValue> valueSerializer) {
    this.path = path;
    this.value = value;
    this.nodeFactory = nodeFactory;
    this.valueSerializer = valueSerializer;
  }

  @Override
  public Node<V> accept(final PathNodeVisitor<V> visitor, final BytesValue path) {
    return visitor.visit(this, path);
  }

  @Override
  public void accept(final NodeVisitor<V> visitor) {
    visitor.visit(this);
  }

  @Override
  public BytesValue getPath() {
    return path;
  }

  @Override
  public Optional<V> getValue() {
    return Optional.of(value);
  }

  @Override
  public BytesValue getRlp() {
    if (rlp != null) {
      final BytesValue encoded = rlp.get();
      if (encoded != null) {
        return encoded;
      }
    }

    final BytesValueRLPOutput out = new BytesValueRLPOutput();
    out.startList();
    out.writeBytesValue(CompactEncoding.encode(path));
    out.writeBytesValue(valueSerializer.apply(value));
    out.endList();
    final BytesValue encoded = out.encoded();
    rlp = new WeakReference<>(encoded);
    return encoded;
  }

  @Override
  public BytesValue getRlpRef() {
    final BytesValue rlp = getRlp();
    if (rlp.size() < 32) {
      return rlp;
    } else {
      return RLP.encodeOne(getHash());
    }
  }

  @Override
  public Bytes32 getHash() {
    if (hash != null) {
      final Bytes32 hashed = hash.get();
      if (hashed != null) {
        return hashed;
      }
    }
    final Bytes32 hashed = keccak256(getRlp());
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  @Override
  public Node<V> replacePath(final BytesValue path) {
    return nodeFactory.createLeaf(path, value);
  }

  @Override
  public String print() {
    return "Leaf:"
        + "\n\tRef: "
        + getRlpRef()
        + "\n\tPath: "
        + CompactEncoding.encode(path)
        + "\n\tValue: "
        + getValue().map(Object::toString).orElse("empty");
  }

  @Override
  public boolean isDirty() {
    return dirty;
  }

  @Override
  public void markDirty() {
    dirty = true;
  }
}
