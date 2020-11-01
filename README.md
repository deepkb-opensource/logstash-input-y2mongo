# Logstash Y2Mongo Input Plugin

This is a logstash input plugin for MongoDB.

It runs a query or aggregate against MongoDB, generate events for logstash pipeline.

This plugin is contributed by Y2 Consulting Inc. (http://deepkb.com). The license is Apache 2.0.

## How To Use

- Install: <p/>
  You will find the gem file under project root (logstash-input-y2mongo-x.x.x.gem) <br/>
  Run the commend to install the gem to logstash: <br/>
  ```sh
  $LS_HOME/bin/logstash-plugin install --no-verify --local logstash-input-y2mongo-x.x.x.gem
  ```
- Sample Usage: <p/>
  In the pipeline config for logstash, use it for input section Eg: <br/>
  
  ```sh
  input {
  	y2mongo {
  		connection_string => "mongodb://localhost:27017"
  		database => "product_db"
  		collection => "products"
  		schedule => "0 * * * * ?"
  		aggregate => '[{$group: {_id: null, count: { $sum: 1 },  priceTotal: { $sum: "$price"}}}]'
  	}
  }
  
  output {
  	stdout {}
  }
  ```
 
## Configuration
 
 **connection_string** : mongodb connections string <br/> 
 &nbsp;&nbsp;If authentication needed, please put username and password in the connection string. <br/> 
 &nbsp;&nbsp;Eg: <br/>
  &nbsp;&nbsp;&nbsp;&nbsp; mongodb://localhost:27017<br/>
  &nbsp;&nbsp;&nbsp;&nbsp; mongodb://username:password@localhost:27017<br/>
 
 **database** :    database name <br/>
 
 **collection** : collection name <br/>
 
 **query** : database query string, optional; <br/>
 &nbsp;&nbsp;MongoDB query String, if omitted, scan the whole collection.
 
 **aggregate** : database aggregate string, optional<br/>
 &nbsp;&nbsp;MongoDB aggregate string. <br/>
 &nbsp;&nbsp;Aggregation support complex data process like stored procedure.
 If you want to look up another collection, de-normalize am array children etc, you can use aggregate instead of query. 
 <br/>
 &nbsp;&nbsp;Ref: https://docs.mongodb.com/manual/aggregation/#aggregation-pipeline
 
 **schedule** : cron string, optional <br/>
 If omitted, run the process once. (Does not make much sense in production env, could be for debug) <br/>
 Examples: 
  
  | Use Case      	| Cron Expression 	|
  |---------------	|-----------------	|
  | Every 30 mins 	| 0 */30 * ? * *  	|
  | Every Hour    	| 0 0 * ? * *      	|
  | Every 2 hours  	| 0 0 */2 ? * *    	| 
  | Every 2 hours  	| 0 0 */2 ? * *    	|
  | Every day at 1am| 0 0 1 * * ?    	|
  
 You can use expression generator: 
 <br>&nbsp;&nbsp;&nbsp;&nbsp;https://www.freeformatter.com/cron-expression-generator-quartz.html <br/>
 Detailed doc please ref to 
 <br>&nbsp;&nbsp;&nbsp;&nbsp;http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html

## How To Build
Clone the project.
<br> Run the following command to generate gem
```sh
gradle --no-daemon gem
```
<br> Run the following command to install gem to logstash before you use it
```sh
$LS_HOME/bin/logstash-plugin install --no-verify --local logstash-input-y2mongo-x.x.x.gem
```
Where LS_HOME is the home directory of you logstash installation.
