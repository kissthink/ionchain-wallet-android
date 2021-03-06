package org.ionc.wallet.transaction;

import org.ionc.wallet.bean.WalletBean;
import org.web3j.utils.Convert;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * describe: 交易的辅助类
 * 交易参数的封装,封装交易参数
 *
 * @author xubinbin
 * @date 2019/03/26
 */
public class TransactionHelper {
    /**
     * 转出钱包
     */
    private WalletBean walletBeanTx;
    /**
     * 转入地址
     */
    private String toAddress;
    /**
     * gas价格,以Gwei为单位
     */
    private int gasPrice;
    /**
     * 转账金额
     */
    private String txValue;

    /**
     * 转出钱包,泳衣获取转出所需相关的必备参数
     * @param walletBeanTx
     * @return
     */
    public TransactionHelper setWalletBeanTx(WalletBean walletBeanTx) {
        this.walletBeanTx = walletBeanTx;
        return this;
    }

    /**
     * 转出地址
     * @param toAddress
     * @return
     */
    public TransactionHelper setToAddress(String toAddress) {
        this.toAddress = toAddress;
        return this;
    }

    /**
     * @param gasPrice
     * @return
     */
    public TransactionHelper setGasPrice(int gasPrice) {
        this.gasPrice = gasPrice;
        return this;
    }

    public TransactionHelper setTxValue(String txValue) {
        this.txValue = txValue;
        return this;
    }

    public WalletBean getWalletBeanTx() {
        return walletBeanTx;
    }

    public String getToAddress() {
        return toAddress;
    }

    /**
     * 将 gasPrice 转为 wei,再转为 BigInteger类型
     * @return 当前的 gasPrice 单位 wei
     */
    public BigInteger getGasPrice() {
        return Convert.toWei(String.valueOf(gasPrice), Convert.Unit.GWEI).toBigInteger();
    }

    /**
     * 将 用户设置的转账金额转为以太坊接口中所需要的数据格式
     * @return 转账金额
     */
    public BigInteger getTxValue() {
        return Convert.toWei(BigDecimal.valueOf(Double.parseDouble(txValue)), Convert.Unit.ETHER).toBigInteger();
    }
}
