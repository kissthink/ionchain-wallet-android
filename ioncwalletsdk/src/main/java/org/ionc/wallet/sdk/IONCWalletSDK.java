package org.ionc.wallet.sdk;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import org.bitcoinj.crypto.ChildNumber;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;
import org.bitcoinj.wallet.DeterministicSeed;
import org.greenrobot.greendao.query.QueryBuilder;
import org.ionc.wallet.sdk.bean.KeystoreBean;
import org.ionc.wallet.sdk.bean.WalletBean;
import org.ionc.wallet.sdk.callback.OnBalanceCallback;
import org.ionc.wallet.sdk.callback.OnCreateWalletCallback;
import org.ionc.wallet.sdk.callback.OnDeletefinishCallback;
import org.ionc.wallet.sdk.callback.OnImportMnemonicCallback;
import org.ionc.wallet.sdk.callback.OnImportPrivateKeyCallback;
import org.ionc.wallet.sdk.callback.OnModifyWalletPassWordCallback;
import org.ionc.wallet.sdk.callback.OnSimulateTimeConsume;
import org.ionc.wallet.sdk.callback.OnTransationCallback;
import org.ionc.wallet.sdk.callback.OnUpdatePasswordCallback;
import org.ionc.wallet.sdk.dao.DaoManager;
import org.ionc.wallet.sdk.dao.EntityManager;
import org.ionc.wallet.sdk.greendaogen.WalletBeanDao;
import org.ionc.wallet.sdk.utils.GsonUtils;
import org.ionc.wallet.sdk.utils.Logger;
import org.ionc.wallet.sdk.utils.MnemonicUtils;
import org.ionc.wallet.sdk.utils.RandomUntil;
import org.ionc.wallet.sdk.utils.SecureRandomUtils;
import org.ionc.wallet.sdk.utils.StringUtils;
import org.ionc.wallet.sdk.utils.ToastUtil;
import org.ionc.wallet.sdk.widget.IONCAllWalletDialogSDK;
import org.web3j.crypto.Bip39Wallet;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.Web3jFactory;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.ChainId;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static org.ionc.wallet.sdk.constant.ConstanParams.GAS_MIN;
import static org.ionc.wallet.sdk.constant.ConstantUrl.IONC_CHAIN_NODE;
import static org.ionc.wallet.sdk.utils.MnemonicUtils.generateMnemonic;
import static org.web3j.crypto.Hash.sha256;

public class IONCWalletSDK {
    private volatile static IONCWalletSDK mInstance;
    private static Web3j web3j;
    public static Context AppContext;
    private static String keystoreDir;
    private static final SecureRandom secureRandom = SecureRandomUtils.secureRandom(); //"https://ropsten.etherscan.io/token/0x92e831bbbb22424e0f22eebb8beb126366fa07ce"


    private Handler mHandler;
    private final String TAG = this.getClass().getSimpleName();

    private BigInteger gas = GAS_MIN;

    /**
     * 通用的以太坊基于bip44协议的助记词路径 （imtoken jaxx Metamask myetherwallet）
     */
    private static String ETH_JAXX_TYPE = "m/44'/60'/0'/0/0";
    public static String ETH_LEDGER_TYPE = "m/44'/60'/0'/0";
    public static String ETH_CUSTOM_TYPE = "m/44'/60'/1'/0/0";


    private MnemonicCode mMnemonicCode = null;
    private final BigInteger gasLimit = Convert.toWei("21000", Convert.Unit.WEI).toBigInteger();
    private BigInteger gasPrice = BigInteger.valueOf(1);


    private IONCWalletSDK() {

    }

    public static IONCWalletSDK getInstance() {
        if (mInstance == null) {
            synchronized (IONCWalletSDK.class) {
                if (mInstance == null) {
                    mInstance = new IONCWalletSDK();
                }
            }
        }
        return mInstance;
    }

    /**
     * 自定义gas
     *
     * @param gas
     */
    public void setGas(BigInteger gas) {
        this.gas = gas;
    }

    public BigInteger getGas() {
        return gas;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }


    /**
     * 初始化钱包
     */
    public void initIONCWalletSDK(Context context) {
        AppContext = context.getApplicationContext();
        web3j = Web3jFactory.build(new HttpService(IONC_CHAIN_NODE));
        AppContext = context;
        mHandler = new Handler(AppContext.getMainLooper());
        try {
            mMnemonicCode = new MnemonicCode(AppContext.getAssets().open("en-mnemonic-word-list.txt"), null);
            keystoreDir = context.getExternalCacheDir() + "/ionchain/keystore";
            Logger.i(TAG, "initIONCWalletSDK: 文件创建成功 keystoreDir = " + keystoreDir);
            //创建keystore路径
            File file = new File(keystoreDir);
            if (!file.exists()) {
                boolean crate = file.mkdirs();
            }
            Logger.i(TAG, "initIONCWalletSDK: 文件创建成功file =" + file.getPath());
        } catch (IOException e) {
            Logger.i(e.getMessage());
        }


        new Thread() {
            /**
             *
             */
            @Override
            public void run() {
                super.run();
                try {
                    try {
                        Logger.i("web3jversion :" + web3j.web3ClientVersion().send().getWeb3ClientVersion());
                    } catch (IOException e) {
                        Logger.e(e.getMessage());
                    }
                    /*
                     * gasPrice
                     *
                     * 1 Gwei = 1e9wei = 1000000000 wei
                     * 1 ETH = 1000000000 GWei
                     * */
                    gasPrice = web3j.ethGasPrice().send().getGasPrice();//Returns the current gas price in wei

                } catch (IOException e) {
                    Logger.i(TAG, "run: " + e.getMessage());
                }

            }
        }.start();
    }


    /**
     * 获取最小费用
     *
     * @return
     */
    public BigDecimal getMinFee() {
        BigInteger fee = gas.multiply(gasPrice);
        return Convert.fromWei(String.valueOf(fee), Convert.Unit.ETHER);
    }

    /**
     * 获取当前进度条下的费用
     *
     * @param currentProgress
     * @return
     */
    public BigDecimal getCurrentFee(int currentProgress) {
        BigInteger fee = gas.multiply(BigInteger.valueOf(currentProgress));
        Log.i(TAG, "getCurrentFee: " + fee);
        /*
         * 从wei到ether
         * */
        return Convert.fromWei(Convert.toWei(String.valueOf(fee), Convert.Unit.GWEI), Convert.Unit.ETHER);
    }

    //创建钱包---借助  importWalletByMnemonicCode
    public void createBip39Wallet(String walletName, String password, final OnImportMnemonicCallback callback) {
        try {
            byte[] initialEntropy = new byte[16];
            secureRandom.nextBytes(initialEntropy);//产生一个随机数
            List<String> mnemonicCode = mMnemonicCode.toMnemonic(initialEntropy);//生成助记词
            importWalletByMnemonicCode(walletName, mnemonicCode, password, callback);
        } catch (MnemonicException.MnemonicLengthException e) {
            callback.onImportMnemonicFailure(e.getMessage());
        }
    }

    //导入钱包--助记词
    public void importWalletByMnemonicCode(String walletName, List<String> mnemonicCode, String password, OnImportMnemonicCallback callback) {
        password = StringUtils.getSHA(password);//加密
        String[] pathArray = ETH_JAXX_TYPE.split("/");
        String passphrase = "";
        long creationTimeSeconds = System.currentTimeMillis() / 1000;
        DeterministicSeed ds = new DeterministicSeed(mnemonicCode, null, passphrase, creationTimeSeconds);
        //种子
        byte[] seedBytes = ds.getSeedBytes();

        if (seedBytes == null) {
            callback.onImportMnemonicFailure("创建种子（钱包）失败");
            return;
        }
        DeterministicKey dkKey = HDKeyDerivation.createMasterPrivateKey(seedBytes);
        for (int i = 1; i < pathArray.length; i++) {
            ChildNumber childNumber;
            if (pathArray[i].endsWith("'")) {
                int number = Integer.parseInt(pathArray[i].substring(0,
                        pathArray[i].length() - 1));
                childNumber = new ChildNumber(number, true);
            } else {
                int number = Integer.parseInt(pathArray[i]);
                childNumber = new ChildNumber(number, false);
            }
            dkKey = HDKeyDerivation.deriveChildKey(dkKey, childNumber);
        }

        ECKeyPair ecKeyPair = ECKeyPair.create(dkKey.getPrivKeyBytes());

        WalletBean walletBean = new WalletBean();
//            WalletFile walletFile = Wallet.create(password, ecKeyPair, 1024, 1); // WalletUtils. .generateNewWalletFile();
        String privateKey = ecKeyPair.getPrivateKey().toString(16);
        String publicKey = ecKeyPair.getPublicKey().toString(16);
        walletBean.setPrivateKey(privateKey);
        walletBean.setPublickey(publicKey);
        Logger.i("私钥： " + privateKey);
        try {
            String keystore = WalletUtils.generateWalletFile(password, ecKeyPair, new File(keystoreDir), false);
            keystore = keystoreDir + "/" + keystore;
            walletBean.setKeystore(keystore);
            Logger.i("钱包keystore： " + keystore);
            if (StringUtils.isEmpty(walletName)) {
                walletName = generateNewWalletName();
            }
            walletBean.setName(walletName);
//            String addr1 = walletFile.getAddress();
            String addr2 = Keys.getAddress(ecKeyPair);
            String walletAddress = Keys.toChecksumAddress(addr2);
            walletBean.setAddress(Keys.toChecksumAddress(walletAddress));//设置钱包地址
            Logger.i("钱包地址： " + walletAddress);
            walletBean.setPassword(password);


            StringBuilder sb = new StringBuilder();
            for (String mnemonic : mnemonicCode) {
                sb.append(mnemonic);
                sb.append(" ");
            }
            Logger.i("助记词 === " + sb.toString());
            String mnemonicWord = sb.toString();
            walletBean.setMnemonic(mnemonicWord);
            callback.onImportMnemonicSuccess(walletBean);
        } catch (CipherException | IOException e) {
            callback.onImportMnemonicFailure(e.getMessage());
        }

    }

    //导入钱包--KS
    public void importWalletByKeyStore(final String p, final String keystoreContent, final OnCreateWalletCallback callback) {
        final String password = StringUtils.getSHA(p);//加密
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    KeystoreBean keystoreBean = GsonUtils.gsonToBean(keystoreContent, KeystoreBean.class);
                    if (keystoreBean == null) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onCreateFailure("请输入正确的keystore!");
                            }
                        });
                        return;
                    }
                    final WalletBean bean = new WalletBean();

                    String path = keystoreDir + "/" + getWalletFileName(keystoreBean.getAddress());
                    File file = new File(path);
                    if (!file.exists()) {
                        file.createNewFile();
                    }
                    FileOutputStream out = new FileOutputStream(file, false); //如果追加方式用true,此处覆盖
//                    StringBuffer sb = new StringBuffer();
                    byte[] bytes = keystoreContent.getBytes();
                    out.write(bytes);
//                out.write(sb.toString().getBytes("utf-8"));//注意需要转换对应的字符集
                    out.close();
                    //创建钱包
                    Credentials credentials = WalletUtils.loadCredentials(password, path);
                    ECKeyPair keyPair = credentials.getEcKeyPair();
                    String walletname = "新增钱包" + RandomUntil.getSmallLetter(3);
                    bean.setName(walletname);
                    bean.setPrivateKey(keyPair.getPrivateKey().toString(16));//私钥
                    bean.setPublickey(keyPair.getPublicKey().toString(16));//公钥
                    bean.setAddress("0x" + Keys.getAddress(keyPair)); //地址
                    bean.setPassword(password); //密码
                    bean.setKeystore(path);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onCreateSuccess(bean);
                        }
                    });
                } catch (IOException | CipherException | NullPointerException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Logger.e(e.getMessage() + "  " + password);
                            callback.onCreateFailure(e.getMessage());
                        }
                    });
                }
            }
        }.start();

    }

    //导入钱包--私钥
    public void importPrivateKey(final String privateKey, final String p, final OnCreateWalletCallback callback) {

        try {
            String passwrd = StringUtils.getSHA(p);//加密
            final WalletBean wallet = new WalletBean();
            String walletname = "新增钱包" + RandomUntil.getSmallLetter(3);
            BigInteger key = new BigInteger(privateKey, 16);
            ECKeyPair keyPair = ECKeyPair.create(key);
            String private_key = keyPair.getPrivateKey().toString(16);
            wallet.setPrivateKey(private_key);
            wallet.setPublickey(keyPair.getPublicKey().toString(16));
            wallet.setAddress("0x" + Keys.getAddress(keyPair));
            wallet.setName(walletname);
            wallet.setMnemonic("");
            wallet.setPassword(passwrd);
            String keystore = WalletUtils.generateWalletFile(passwrd, keyPair, new File(keystoreDir), false);
            keystore = keystoreDir + "/" + keystore;
            wallet.setKeystore(keystore);

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onCreateSuccess(wallet);
                }
            });

        } catch (CipherException | IOException e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onCreateFailure(e.getMessage());
                }
            });
        }
    }

    //更新密码
    public void updatePasswordAndKeyStore(final WalletBean wallet, String newPassword, final OnUpdatePasswordCallback callback) {
        try {
            newPassword = StringUtils.getSHA(newPassword);//加密
            BigInteger key = new BigInteger(wallet.getPrivateKey(), 16);
            ECKeyPair keyPair = ECKeyPair.create(key);

            String keystore = WalletUtils.generateWalletFile(newPassword, keyPair, new File(keystoreDir), false);
            keystore = keystoreDir + "/" + keystore;
            wallet.setKeystore(keystore);
            wallet.setPassword(newPassword);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onUpdatePasswordSuccess(wallet);
                }
            });

        } catch (CipherException | NumberFormatException | IOException e) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    callback.onUpdatePasswordFailure(e.getMessage());
                }
            });
        }
    }

    //移除私钥
    public void removeWalletPrivateKey(WalletBean wallet) {

        try {
            wallet.setPrivateKey("");
            EntityManager.getInstance().getWalletDao().update(wallet);
        } catch (Throwable e) {
            Logger.e("e", "getAllWallet");
        }
    }

    //修改密码  没有所谓的 修改的密码实现是 利用私匙重新生成一个keystore
    public void modifyPassWord(final WalletBean wallet, final String newPassWord, final OnModifyWalletPassWordCallback callback) {

        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    String ps = wallet.getPassword();
                    String ks = wallet.getKeystore();
                    Logger.i("ps = " + ps);
                    Logger.i("ks = " + ks);
                    Credentials credentials = WalletUtils.loadCredentials(ps, ks);
                    Logger.i("credentials = " + credentials);
                    Logger.i("credentials.getAddress() = " + credentials.getAddress());
                    Logger.i("wallet.getAddress() = " + wallet.getAddress());
                    if (!credentials.getAddress().equals(wallet.getAddress().toLowerCase())) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onModifyFailure("修改失败！");
                            }
                        });
                        return;
                    }
                    wallet.setPrivateKey(credentials.getEcKeyPair().getPrivateKey().toString(16));
                    String keystore;
                    String key = wallet.getPrivateKey();
                    Logger.i("key", "importWallt: " + key);
                    BigInteger privateKeyBig = new BigInteger(key, 16);
                    ECKeyPair ecKeyPair = ECKeyPair.create(privateKeyBig);
                    keystore = WalletUtils.generateWalletFile(newPassWord, ecKeyPair, new File(keystoreDir), false);
                    keystore = keystoreDir + "/" + keystore;

                    Logger.i("new keystore ==>" + keystore);

                    //发生更换了
                    if (null != wallet.getKeystore() && !wallet.getKeystore().equals(keystore)) {
                        String old = wallet.getKeystore();
                        Logger.i("old keystore ==>" + old);
                        //删除旧的keystore
                        File file = new File(old);
                        if (file.exists()) {
                            file.delete();
                        }
                    }
                    wallet.setKeystore(keystore);
                    wallet.setPassword(newPassWord);
                    removeWalletPrivateKey(wallet);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onModifySuccess(wallet);
                        }
                    });
                } catch (final Exception e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onModifyFailure(e.getMessage());
                        }
                    });

                }

            }
        }.start();
    }

    //导出私钥-- 异步获取
    public void exportPrivateKey(final String keystore, final String pwd_dao, final OnImportPrivateKeyCallback callback) {

        new Thread() {
            @Override
            public void run() {
                super.run();

                try {
                    Credentials credentials = WalletUtils.loadCredentials(pwd_dao, keystore);
                    final String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onImportPriKeySuccess(privateKey);
                        }
                    });
                } catch (final Exception e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onImportPriKeySuccess(e.getMessage());
                        }
                    });
                }

            }
        }.start();
    }

    @NonNull
    private String generateNewWalletName() {
        char letter1 = (char) (int) (Math.random() * 26 + 97);
        char letter2 = (char) (int) (Math.random() * 26 + 97);
        String walletName = String.valueOf(letter1) + String.valueOf(letter2) + "-新钱包";
        while (getWalletByName(walletName) != null) {
            letter1 = (char) (int) (Math.random() * 26 + 97);
            letter2 = (char) (int) (Math.random() * 26 + 97);
            walletName = String.valueOf(letter1) + String.valueOf(letter2) + "-新钱包";
        }
        return walletName;
    }

    private static boolean createParentDir(File file) {
        //判断目标文件所在的目录是否存在
        if (!file.getParentFile().exists()) {
            //如果目标文件所在的目录不存在，则创建父目录
            System.out.println("目标文件所在目录不存在，准备创建");
            if (!file.getParentFile().mkdirs()) {
                System.out.println("创建目标文件所在目录失败！");
                return false;
            }
        }
        return true;
    }


    private Bip39Wallet generateBip39Wallet(String password, File destinationDirectory)
            throws CipherException, IOException {
        byte[] initialEntropy = new byte[16];
        secureRandom.nextBytes(initialEntropy);//产红一个随机数

        //生成助记词
        String mnemonic = generateMnemonic(initialEntropy);
        Logger.i("mnemonic", "generateBip39Wallet: " + mnemonic);
        //根据助记词生成种子
        byte[] seed = MnemonicUtils.generateSeed(mnemonic, password);
        //根据种子生成秘钥对
        ECKeyPair privateKey = ECKeyPair.create(sha256(seed));
        //完成钱包的创建
        String walletFile = WalletUtils.generateWalletFile(password, privateKey, destinationDirectory, false);

        return new Bip39Wallet(walletFile, mnemonic);
    }


    /**
     * 创建文件名
     *
     * @param walletAddress
     * @return
     */
    private String getWalletFileName(String walletAddress) {
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat dateFormat = new SimpleDateFormat("'UTC--'yyyy-MM-dd'T'HH-mm-ss.SSS'--'");
        return dateFormat.format(new Date()) + walletAddress + ".json";
    }

    /**
     * 返回账户信息
     * 私钥
     * 公钥
     *
     * @param keyPair
     * @return
     */
    private String[] getAccountTuple(ECKeyPair keyPair) {
        return new String[]{
                keyPair.getPrivateKey().toString(16),//私钥
                keyPair.getPublicKey().toString(16),//公钥
                Keys.getAddress(keyPair)//地址
        };
    }


    /**
     * @param walletBean 钱包地址
     */
    public void getAccountBalance(final WalletBean walletBean, final OnBalanceCallback callback) {


        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    BigInteger balance = web3j.ethGetBalance(walletBean.getAddress(), DefaultBlockParameterName.LATEST).send().getBalance();

                    BigDecimal balacne = Convert.fromWei(balance.toString(), Convert.Unit.ETHER);
                    balacne = balacne.setScale(4, BigDecimal.ROUND_DOWN);
                    Logger.i("余额" + balacne);
                    int a = balacne.compareTo(BigDecimal.valueOf(10));
                    walletBean.setBalance(String.valueOf(balacne));
                    removeWalletPrivateKey(walletBean);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onBalanceSuccess(walletBean);
                        }
                    });
                } catch (final IOException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onBalanceFailure(e.getMessage());
                        }
                    });
                }
            }
        }.start();
    }


    /**
     * 钱包转账
     *
     * @param password 钱包密码
     * @param from     转出地址
     * @param to       转入地址
     * @param txPrice  交易费用
     * @param keystore keystore
     * @param account  转账金额
     */
    public void transaction(final String from, final String to, final BigDecimal txPrice, final String password,
                            final String keystore, final double account, final OnTransationCallback callback) {
        new Thread() {
            /**
             *
             */
            @SuppressWarnings("UnnecessaryLocalVariable")
            @Override
            public void run() {
                super.run();

                EthGetTransactionCount ethGetTransactionCount = null;
                try {
                    ethGetTransactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.PENDING).send();
                } catch (final IOException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTxFailure(e.getMessage());
                        }
                    });
                    return;
                }
                if (ethGetTransactionCount == null) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTxFailure("null......");
                        }
                    });
                    return;
                }
                BigInteger nonce = ethGetTransactionCount.getTransactionCount();
//                BigInteger gasPrice = Convert.toWei(BigDecimal.valueOf(3), Convert.Unit.GWEI).toBigInteger();
//                BigInteger gasLimit = BigInteger.valueOf(30000);

                BigInteger gasPrice = txPrice.toBigInteger();
                String toAddress = to.toLowerCase();
                BigInteger value = Convert.toWei(BigDecimal.valueOf(account), Convert.Unit.ETHER).toBigInteger();
                String data = "";
                String signedData;
                try {
//                    ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
//                    Credentials credentials = Credentials.create(ecKeyPair);

                    Credentials credentials = WalletUtils.loadCredentials(password, keystore);
                    String privateKey = credentials.getEcKeyPair().getPrivateKey().toString(16);
                    byte[] signedMessage;


                    RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, value);
                    signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                    signedData = Numeric.toHexString(signedMessage);


                    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedData).send();
                    final String hashTx = ethSendTransaction.getTransactionHash();//转账成功hash 不为null
                    if (!TextUtils.isEmpty(hashTx)) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.OnTxSuccess(hashTx);
                            }
                        });
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onTxFailure("null......");
                            }
                        });
                    }
                } catch (final IOException | CipherException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTxFailure(e.getMessage());
                        }
                    });
                }
            }
        }
                .start();
    }

    /**
     * 钱包转账
     *
     * @param password 钱包密码
     * @param from     转出地址
     * @param to       转入地址
     * @param gasPrice 交易费用单价
     * @param keystore keystore
     * @param value    转账金额
     */
    public void transaction(final String from, final String to, final BigInteger gasPrice, final String password,
                            final String keystore, final BigInteger value, final OnTransationCallback callback) {
        new Thread() {
            /**
             * BigInteger value = Convert.toWei(BigDecimal.valueOf(account), Convert.Unit.ETHER).toBigInteger();
             */
            @SuppressWarnings("UnnecessaryLocalVariable")
            @Override
            public void run() {
                super.run();
                try {
                    EthGetTransactionCount ethGetTransactionCount = web3j.ethGetTransactionCount(from, DefaultBlockParameterName.PENDING).send();
                    BigInteger nonce = ethGetTransactionCount.getTransactionCount();
                    String toAddress = to.toLowerCase();
                    Credentials credentials = WalletUtils.loadCredentials(password, keystore);
                    RawTransaction rawTransaction = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, value);
                    byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
                    String signedData = Numeric.toHexString(signedMessage);
                    EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(signedData).send();
                    final String hashTx = ethSendTransaction.getTransactionHash();//转账成功hash 不为null
                    if (!TextUtils.isEmpty(hashTx)) {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.OnTxSuccess(hashTx);
                            }
                        });
                    } else {
                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                callback.onTxFailure("交易失败");
                            }
                        });
                    }
                } catch (final IOException | CipherException |NullPointerException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            callback.onTxFailure(e.getMessage());
                        }
                    });
                }
            }
        }
                .start();
    }

    /**
     * 签名交易
     */
    private String signTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to,
                                   BigInteger value, String data, byte chainId, String privateKey) {
        byte[] signedMessage;
        RawTransaction rawTransaction = RawTransaction.createTransaction(
                nonce,
                gasPrice,
                gasLimit,
                to,
                value,
                data);

        if (privateKey.startsWith("0x")) {
            privateKey = privateKey.substring(2);
        }
        ECKeyPair ecKeyPair = ECKeyPair.create(new BigInteger(privateKey, 16));
        Credentials credentials = Credentials.create(ecKeyPair);

        if (chainId > ChainId.NONE) {
            signedMessage = TransactionEncoder.signMessage(rawTransaction, chainId, credentials);
        } else {
            signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        }

        return Numeric.toHexString(signedMessage);
    }


    /**
     * 模拟耗时操作
     */
    public void simulateTimeConsuming(final OnSimulateTimeConsume simulateTimeConsume) {
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    sleep(3000);
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            simulateTimeConsume.onSimulateFinish();
                        }
                    });
                } catch (InterruptedException e) {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            simulateTimeConsume.onSimulateFinish();
                        }
                    });
                }
            }
        }.start();
    }

    public WalletBean getWalletByName(String name) {
        WalletBean wallet = null;
        List<WalletBean> list = DaoManager.getInstance()
                .getSession()
                .getWalletBeanDao()
                .queryBuilder()
                .where(WalletBeanDao.Properties.Name.eq(name))
                .list();
        if (list.size() > 0) {
            wallet = list.get(0);
        }
        return wallet;
    }

    public WalletBean getWalletByAddress(String adress) {
        WalletBean wallet = null;
        List<WalletBean> list = DaoManager.getInstance().getSession().getWalletBeanDao().queryBuilder().where(WalletBeanDao.Properties.Address.eq(adress)).list();
        if (list.size() > 0) {
            wallet = list.get(0);
        }
        return wallet;
    }


    /**
     * 查询 所有的钱包
     */
    public List<WalletBean> getAllWallet() {
        List<WalletBean> walletList = null;
        try {
            QueryBuilder.LOG_SQL = true;
            QueryBuilder.LOG_VALUES = true;
            QueryBuilder<WalletBean> qb = EntityManager.getInstance().getWalletDao().queryBuilder();
            walletList = qb.orderDesc(WalletBeanDao.Properties.Id).list();
        } catch (Throwable e) {
            return walletList;
        }
        return walletList;
    }


    /**
     * 保存钱包,保存前,检查数据库是否存在钱包,如果没有则将该钱包设置为首页展示钱包
     *
     * @param wallet 钱包
     * @return
     */
    public long saveWallet(WalletBean wallet) {
        if (IONCWalletSDK.getInstance().getAllWallet() == null || IONCWalletSDK.getInstance().getAllWallet().size() == 0) {
            wallet.setIsMainWallet(true);
        }
        //私钥不存储于数据库中

        wallet.setPrivateKey("");
        return EntityManager.getInstance().getWalletDao().insertOrReplace(wallet);
    }

    /**
     * 删除钱包,删除前
     *
     * @param wallet
     */
    public void deleteWallet(WalletBean wallet, OnDeletefinishCallback deletefinishCallback) {
        File file = new File(wallet.getKeystore());
        if (file.exists()) {
            file.delete();
        }
        //私钥不存储于数据库中
        EntityManager.getInstance().getWalletDao().delete(wallet);
        deletefinishCallback.onDeleteFinish();
    }

    //获取最新的 最老的钱包
    public WalletBean getWalletTop() {
        //私钥不存储于数据库中
        WalletBean wallet = null;
        List<WalletBean> list = EntityManager.getInstance().getWalletDao().queryBuilder().limit(1).list();
        if (list.size() > 0) {
            wallet = list.get(0);
        }
        return wallet;
    }


    /**
     * 支付
     *
     * @param activity
     * @param callback
     */
    public void transactionDialog(Activity activity, IONCAllWalletDialogSDK.OnTxResultCallback callback) {
        List<WalletBean> beans = getAllWallet();
        if (beans == null || beans.size() <= 0) {
            ToastUtil.showLong("钱包为空！");
            return;
        }
        new IONCAllWalletDialogSDK(activity, beans, callback).show();
    }

    /**
     * @return 首页展示的钱包
     */
    public WalletBean getMainWallet() {
        List<WalletBean> beans = getAllWallet();
        WalletBean walletBean = null;
        int count = beans.size();
        for (int i = 0; i < count; i++) {
            if (beans.get(i).getIsMainWallet()) {
                walletBean = beans.get(i);
                break;
            }
        }
        return walletBean;
    }

    public void release() {
        mInstance = null;
    }
}