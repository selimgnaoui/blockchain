package blockchain;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private TransactionPool transactionPool;
    private int maxHeight;
    private List<List<Block>> blocks;
    private Map<ByteArrayWrapper, Integer> heights;
    private Map<ByteArrayWrapper, UTXOPool> UTXOPools;

    
    public BlockChain(Block genesisBlock) {
        this.transactionPool = new TransactionPool();
        this.maxHeight = 0;
        List<Block> treeRoot = new ArrayList<>();
        treeRoot.add(genesisBlock);
        this.blocks = new ArrayList<>();
        this.blocks.add(treeRoot);
        this.heights = new HashMap<>();
        ByteArrayWrapper wrap = new ByteArrayWrapper(genesisBlock.getHash());
        this.heights.put(wrap, 0);

        this.UTXOPools = new HashMap<>();
        UTXOPool utxoPool = new UTXOPool();
        addCoinbaseToUTXOPool(utxoPool, genesisBlock.getCoinbase());
        this.UTXOPools.put(wrap, utxoPool);
    }

   
    public Block getMaxHeightBlock() {
        return blocks.get(maxHeight).get(0);
    }

   
    public UTXOPool getMaxHeightUTXOPool() {
        return UTXOPools.get(new ByteArrayWrapper(this.getMaxHeightBlock().getHash()));
    }

   
    public TransactionPool getTransactionPool() {
        return transactionPool;
    }


    public boolean addBlock(Block block) {
        // If a genesis block is mined or received, then reject
        if (block.getPrevBlockHash() == null) {
            return false;
        }

        ByteArrayWrapper previousBlockWrapper = new ByteArrayWrapper(block.getPrevBlockHash());
        // If the previous block doesn't exist, then reject
        if (!heights.containsKey(previousBlockWrapper)) {
            return false;
        }

        int height = heights.get(previousBlockWrapper) + 1;
        // If the height is below the cut off, then reject
        if (height <= maxHeight - CUT_OFF_AGE) {
            return false;
        }

        UTXOPool previousUTXOPool = UTXOPools.get(previousBlockWrapper);
        TxHandler txHandler = new TxHandler(previousUTXOPool);
        ArrayList<Transaction> possibleTxs = block.getTransactions();
        Transaction[] txs = txHandler.handleTxs(possibleTxs.toArray(new Transaction[0]));
    
        if (txs.length < possibleTxs.size()) {
            return false;
        }

       
        if (height > maxHeight) {
            List<Block> newLevelList = new ArrayList<>();
            newLevelList.add(block);
            blocks.add(newLevelList);
            maxHeight = height;
        } else {
            blocks.get(height).add(block);
        }
        ByteArrayWrapper currentBlockWrapper = new ByteArrayWrapper(block.getHash());
        heights.put(currentBlockWrapper, height);
        UTXOPool currentUTXOPool = txHandler.getUTXOPool();
        addCoinbaseToUTXOPool(currentUTXOPool, block.getCoinbase());
        UTXOPools.put(currentBlockWrapper, currentUTXOPool);
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        transactionPool.addTransaction(tx);
    }

    
    private void addCoinbaseToUTXOPool(UTXOPool pool, Transaction tx) {
        ArrayList<Transaction.Output> outputs = tx.getOutputs();
        for (int i = 0; i < outputs.size(); i++) {
            pool.addUTXO(new UTXO(tx.getHash(), i), outputs.get(i));
        }
    }
}