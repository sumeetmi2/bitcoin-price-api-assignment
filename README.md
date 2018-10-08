# bitcoin-price-api-assignment
bitcoin-price-assignment

-- API to query bitcoin prices (lastweek, lastmonth, custom date range, moving average and predict prices)

## Setup
```
git clone https://github.com/sumeetmi2/bitcoin-price-api-assignment.git
sbt clean compile
sbt dist
cd bitcoin-price-api/target/universal
unzip bitcoin-price-api-0.1.zip 
chmod u+x bitcoin-price-api-0.1/bin/bitcoin-price-api
bitcoin-price-api-0.1/bin/bitcoin-price-api com.bitcoin.price.app.PriceApp
```

## Swagger Page
```http://localhost:8911/```

