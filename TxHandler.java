package blockchain;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.ArrayList;

public class TxHandler {
    
    private UTXOPool utxoPool;

    public UTXOPool getUtxoPool() {
        return utxoPool;
    }

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public void setUtxoPool(UTXOPool utxoPool) {
        this.utxoPool = utxoPool;
    }

    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid,
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        double sumInputs = 0.0;
        double sumOutputs = 0.0;
        UTXOPool alreadySeenUTXOs = new UTXOPool();
        ArrayList<Transaction.Input> allInputsOfTx = tx.getInputs();
        for(int i = 0; i < allInputsOfTx.size(); i++){
            Transaction.Input oneInputOfTx;
            oneInputOfTx = allInputsOfTx.get(i);
            UTXO utxo = new UTXO(oneInputOfTx.prevTxHash, oneInputOfTx.outputIndex);
            Transaction.Output utxoOutput = this.utxoPool.getTxOutput(utxo);
            if (utxoOutput == null){
                return false;
            }
            PublicKey recipientsAddress = utxoOutput.address;
            if(!(oneInputOfTx.signature == null) && !Crypto.verifySignature(recipientsAddress, tx.getRawDataToSign(i), oneInputOfTx.signature)){
                return false;
            }
            if(alreadySeenUTXOs.contains(utxo)){
                return false;
            }
            alreadySeenUTXOs.addUTXO(utxo, utxoOutput);
            
            sumInputs += utxoOutput.value;
        }
        ArrayList<Transaction.Output> allOutputsOfTx = tx.getOutputs();
        for (Transaction.Output oneOutputofTx : allOutputsOfTx){
            if(oneOutputofTx.value < 0.0){
                return false;
            }
            sumOutputs += oneOutputofTx.value;
        }
        if(sumInputs >= sumOutputs){
            return true;
        }
        return false;
    }

    
    /**
     * Handles each epoch by receiving an unordered array of proposed transactions,
     * (1) checking each transaction for correctness,
     * (2) updating the current UTXO pool as appropriate, and
     * (3) returning a mutually valid array of accepted transactions.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        ArrayList<Transaction> acceptedTXs = new ArrayList<>();
        boolean addedNewUTXO;
        do{
            addedNewUTXO = false;
            for(Transaction tx : possibleTxs){
                if(isValidTx(tx)){
                    acceptedTXs.add(tx);
                    addedNewUTXO = true;
                    for(Transaction.Input txInput : tx.getInputs()){
                        UTXO utxo = new UTXO(txInput.prevTxHash, txInput.outputIndex);
                        utxoPool.removeUTXO(utxo);
                    }
                    for(int i = 0; i < tx.getOutputs().size(); i++){
                        Transaction.Output txOutput = tx.getOutput(i);
                        UTXO utxo = new UTXO(tx.getHash(), i);
                        utxoPool.addUTXO(utxo, txOutput);
                    }
                }
            }
        }while(addedNewUTXO);
        return acceptedTXs.toArray(new Transaction[acceptedTXs.size()]);
    }
    
    public UTXOPool getUTXOPool() {
        return utxoPool;
    }
  
}

