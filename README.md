# skrott
POC for BLE-kommunikasjon med lesestav fra Lexit. Grisekode hvor alt ligger i MainActivity. 

Lesestaven kommuniserer via Bluetooth Low Energy. Lesestaven leser både RFID 125Khz (skrottkrok) og FDXB-S (øremerke)

## Bluetooth Low Energy
En må subscribe på: 

```
Service 0003cdd0-0000-1000-8000-00805f9b0131

Characteristic 0003cdd1-0000-1000-8000-00805f9b0131
  
```

## Data
i UTF-8 returneres de to formatene (125khz og øremerke) i stringer med prefix for å indikere typen. 
F.eks:
```:ID64=041A4CA761``` og ```:FDXB-S=578273873720217```

Med newline på slutten.
