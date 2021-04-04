
## Description
todo

rpc runs on http://localhost:8181/jsonrpc


##RPC Commands

```json
 {
     "id":1,
     "method":"FullNode.sync",
     "params":[]
 }
 ```

```json

 {
     "id":1,
     "method":"FullNode.chain",
     "params":[]
 }
 ```

```json
 {
     "id":1,
     "method":"FullNode.getWalletByAddress",
     "params":["ef48ae678209e1c52157d247cb7b34a718c8ab77b35fac063d841122cc25bf29"]
 }
 ```

```json
 {
     "id":1,
     "method":"FullNode.getWalletByAddressRaw",
     "params":["ef48ae678209e1c52157d247cb7b34a718c8ab77b35fac063d841122cc25bf29"]
 }
 ```

```json
 {
     "id":1,
     "method":"FullNode.unfinishedTransactions",
     "params":[]
 }
 ```

```json
 {
     "id":1,
     "method":"FullNode.getTransactionById",
     "params":["c6e99dbb-67c5-46fb-a4fa-e7ab6c354764"]
 }
 ```

```json
 {
     "id":1,
     "method":"FullNode.getBlockByHash",
     "params":["b34479469da2f45670c92a1b70de383708dc8a7fe43c82b1656cdbcb520f4bdf"]
 }
 ```

```json
 {
     "id":1,
     "method":"FullNode.createMnemonics",
     "params":[]
 }
 ```

```json
 {
     "id":1,
     "method":"FullNode.createWallet",
     "params":[]
 }
 ```
 ```json
 {
     "id":1,
     "method":"FullNode.createPool",
     "params":[{
         "pool":"somepoolid",
         "amount":0.01,
         "privKey":"someprivkey",
         "pubKey":"soepubkey"
     }]
 }
``` 

```json
{
  "id":1,
  "method":"FullNode.validators",
  "params":["e01797f44dbd0e255518a5ebbacceceb6409807b9a0b80bfdcaff87e55abac75"]
}
```


```json
{
  "id":1,
  "method":"FullNode.signWallet",
  "params":["elder twelve guard trigger drama fashion hub trial song lens solve wisdom"]
}
```


```json
{
  "id":1,
  "method":"FullNode.freezes",
  "params":[]
}
```


```json
{
  "id":1,
  "method":"FullNode.transact",
  "params":[{
    "pubKey":"",
    "privKey":"",
    "amount":0.1,
    "to":"TOPUBLICKEY",
    "type":"transaction"
  }]
}
```

```json
{
  "id":1,
  "method":"FullNode.calculateFee",
  "params":[{
    "amount":20.2
  }]
}
```



