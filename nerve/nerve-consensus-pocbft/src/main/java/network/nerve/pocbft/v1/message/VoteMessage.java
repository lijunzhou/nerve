package network.nerve.pocbft.v1.message;

import io.nuls.base.RPCUtil;
import io.nuls.base.basic.AddressTool;
import io.nuls.base.basic.NulsByteBuffer;
import io.nuls.base.basic.NulsOutputStreamBuffer;
import io.nuls.base.data.BaseBusinessMessage;
import io.nuls.base.data.NulsHash;
import io.nuls.base.signture.BlockSignature;
import io.nuls.core.constant.ToolsConstant;
import io.nuls.core.crypto.UnsafeByteArrayOutputStream;
import io.nuls.core.exception.NulsException;
import io.nuls.core.log.Log;
import io.nuls.core.parse.SerializeUtils;
import network.nerve.pocbft.constant.ConsensusConstant;
import network.nerve.pocbft.model.bo.Chain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 投票信息
 * Vote message
 *
 * @author tag
 * 2019/10/28
 */
public class VoteMessage extends BaseBusinessMessage {
    /**
     * 投票的区块高度
     */
    private long height;

    /**
     * 区块所在轮次
     */
    private long roundIndex;

    /**
     * 区块出块下标
     */
    private int packingIndexOfRound;

    /**
     * 共识轮次开始时间
     */
    private long roundStartTime;

    /**
     * 第几轮投票,一个区块确认可能会尽力多轮投票，如果第一轮所有共识节点未达成一致会进入第二轮投票
     */
    private long voteRoundIndex = 1;

    /**
     * 第几阶段投票，每轮投票分两个阶段，第一阶段是对区块签名投票，第二阶段是对第一阶段结果投票
     */
    private byte voteStage = 1;

    /**
     * 区块hash
     */
    private NulsHash blockHash;

    /**
     * 区块签名
     */
    private byte[] sign;


    /**
     * 非序列化字段
     */
    private String address;
    private NulsHash voteHash;
    private String sendNode;
    private String rawData;


    public VoteMessage() {
    }

    @Override
    protected void serializeToStream(NulsOutputStreamBuffer stream) throws IOException {
        stream.writeInt64(height);
        stream.writeUint32(roundIndex);
        stream.writeUint16(packingIndexOfRound);
        stream.writeUint32(voteRoundIndex);
        stream.writeByte(voteStage);
        stream.write(blockHash.getBytes());
        stream.writeUint32(roundStartTime);
        stream.writeBytesWithLength(sign);
    }

    public byte[] serializeForDigest() throws IOException {
        ByteArrayOutputStream bos = null;
        try {
            int size = size() - SerializeUtils.sizeOfBytes(sign);
            bos = new UnsafeByteArrayOutputStream(size);
            NulsOutputStreamBuffer buffer = new NulsOutputStreamBuffer(bos);
            if (size == 0) {
                bos.write(ToolsConstant.PLACE_HOLDER);
            } else {
                buffer.writeInt64(height);
                buffer.writeUint32(roundIndex);
                buffer.writeUint16(packingIndexOfRound);
                buffer.writeUint32(voteRoundIndex);
                buffer.writeByte(voteStage);
                buffer.write(blockHash.getBytes());
                buffer.writeUint32(roundStartTime);
            }
            return bos.toByteArray();
        } finally {
            if (bos != null) {
                bos.close();
            }
        }
    }

    @Override
    public void parse(NulsByteBuffer byteBuffer) throws NulsException {
        this.height = byteBuffer.readInt64();
        this.roundIndex = byteBuffer.readUint32();
        this.packingIndexOfRound = byteBuffer.readUint16();
        this.voteRoundIndex = byteBuffer.readUint32();
        this.voteStage = byteBuffer.readByte();
        this.blockHash = byteBuffer.readHash();
        this.roundStartTime = byteBuffer.readUint32();
        this.sign = byteBuffer.readByLengthByte();

    }

    @Override
    public int size() {
        int size = 16;
        size += 36 + 3;
        size += SerializeUtils.sizeOfBytes(sign);
        return size;
    }

    public long getVoteRoundIndex() {
        return voteRoundIndex;
    }

    public void setVoteRoundIndex(long voteRoundIndex) {
        this.voteRoundIndex = voteRoundIndex;
    }

    public byte getVoteStage() {
        return voteStage;
    }

    public void setVoteStage(byte voteStage) {
        this.voteStage = voteStage;
    }

    public long getHeight() {
        return height;
    }

    public void setHeight(long height) {
        this.height = height;
    }


    public NulsHash getBlockHash() {
        return blockHash;
    }

    public void setBlockHash(NulsHash blockHash) {
        this.blockHash = blockHash;
    }

    public byte[] getSign() {
        return sign;
    }

    public void setSign(byte[] sign) {
        this.sign = sign;
    }

    public long getRoundIndex() {
        return roundIndex;
    }

    public void setRoundIndex(long roundIndex) {
        this.roundIndex = roundIndex;
    }

    public int getPackingIndexOfRound() {
        return packingIndexOfRound;
    }

    public void setPackingIndexOfRound(int packingIndexOfRound) {
        this.packingIndexOfRound = packingIndexOfRound;
    }

    public long getRoundStartTime() {
        return roundStartTime;
    }

    public void setRoundStartTime(long roundStartTime) {
        this.roundStartTime = roundStartTime;
    }

    public NulsHash getHash() throws IOException {
        if (voteHash == null) {
            voteHash = NulsHash.calcHash(serializeForDigest());
        }
        return voteHash;
    }

    public String getAddress(Chain chain) {
        if (null == address && this.sign != null) {
            BlockSignature bs = new BlockSignature();
            try {
                bs.parse(this.sign, 0);
            } catch (NulsException e) {
                chain.getLogger().error(e);
            }
            this.address = AddressTool.getStringAddressByBytes(AddressTool.getAddress(bs.getPublicKey(), chain.getChainId()));
        }
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMessageKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(height);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(roundStartTime);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(roundIndex);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(packingIndexOfRound);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(voteRoundIndex);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(voteStage);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(address);
        return sb.toString();
    }

    public String getTargetKey() {
        StringBuilder sb = new StringBuilder();
        sb.append(height);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(roundIndex);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(packingIndexOfRound);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(voteRoundIndex);
        sb.append(ConsensusConstant.SEPARATOR);
        sb.append(blockHash.toHex());
        return sb.toString();
    }

    public String getSendNode() {
        return sendNode;
    }

    public void setSendNode(String sendNode) {
        this.sendNode = sendNode;
    }

    public void setRawData(String rawData) {
        this.rawData = rawData;
    }

    public String getRawData() {
        if (null == rawData) {
            try {
                this.rawData = RPCUtil.encode(this.serialize());
            } catch (IOException e) {
                Log.error(e);
            }
        }
        return rawData;
    }
}
