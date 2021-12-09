# Exploring the Dataset
The sqlite database containing all the computed data for this project (~30GB) can be downloaded from here:
https://1drv.ms/u/s!AlzPN94u8PvZgpsi9VYBQif5t7LMAA?e=UfTSD6

The best way to explore and work with this data is using the freely available:
https://sqlitebrowser.org/

## Useful SQL queries & processed data

### Get Frequent sub-sequence results for a particular sequencer
Get frequent sub-sequences produced by the RelativeTimeSensitive sequncer, ordered starting with the most frequent.

```sql
select * from FREQUENT_SUBSEQUENCES where ExecutionId = "1d2923ae-5ae2-40cc-88ef-a5d3d5c0ae31" AND Sequence like "%i%" order by Frequency desc;
```

For results from the:
* `OneToOne` sequencer use ExecutionId `cfab4fce-9648-4747-92a3-c61e77eaad35`.
* `RelativeTimeSensitive` sequncer use ExecutionId `1d2923ae-5ae2-40cc-88ef-a5d3d5c0ae31`
* `TimeSensitive5` sequencer use ExecutionId `d9729ac3-e36b-490a-81cc-55bc7d84f0b1`

Results corresponding with this query and used for tables 2,4,5, and 6 in the final report are available in the following files:

* `data/fss-onetoone.csv`
* `data/fss-relativetimesensitive.csv`
* `data/fss-timesensitive5.csv`

### Get silhouette index results for a particular sequencer

```sql
Select SilhouetteIndex from CLUSTERING_RECORDS where SourceExecutionId = "cfab4fce-9648-4747-92a3-c61e77eaad35";
```

For results from the:
* `OneToOne` sequencer use SourceExecutionId `cfab4fce-9648-4747-92a3-c61e77eaad35`
* `RelativeTimeSensitive` sequencer use SourceExecutionId `1d2923ae-5ae2-40cc-88ef-a5d3d5c0ae31`
* `TimeSensitive5` sequencer use SourceExecutionId `d9729ac3-e36b-490a-81cc-55bc7d84f0b1`

Results corresponding with this query and used for table 1 in the final report are available in the following files:

* `data/onetoone-silhouette-results.csv`
* `data/relativetimesensitive-silhouette-results.csv`
* `data/timesensitive5-silhouette-results.csv`

*Note: Standard deviation and mean information found in the above files was computed in Excel with the query results.*

### Return the parameter sweep results for `kappa` and `K`

```sql
Select * from CLUSTERING_RECORDS ORDER by SilhouetteIndex desc;
```

This query was used to create figures 1&2 in the report. The figures themselves are taken straight from the https://sqlitebrowser.org/ tool.

The query result is available in:

`data/clustering-records.csv`

*Note: There are 196 rows in the csv file while only 162 are mentioned in the report. This is because results for kappa = 20 were not fully computed as a result of time constraints.*


# Recreating the experiment 

To recreate the experiment you'll have to:

1. Load KaVE Event data into a sqlite database using the **Loader**
2. Build string sequence representations of the activity interval sequnces to mine using the **Sequence Builder**
3. Mine frequent sub-sequences (FSS) using the **Sequence Miner**
4. Export FSS from step 3 with the **Sequence Miner** into the correct input format for step 5.
5. Produce the SGT embeddings for the mined FSS using the https://github.com/aianta/activity-mining-sgt python scripts.
6. Combine the exported file from step 4 with the output of step 5 to create clusters using **Compute Clusters**


### Using Docker

Each component is provided as a docker container for your convenience, the should automatically be pulled from docker hub when invoked with the `docker run` command.

Execution of some components depends on passing in input files. Inside every docker container we've created a path `/usr/app/mounted`.

Clone this repository and mount the working directory to the docker image uisng the volume commadn like so:

`docker run --volume <your local working dir>:/usr/app/mounted aianta/<image> <args>`.

Keep this volume mounting in mind when specifying file paths. For example you will need to download and extract the KaVE Events dataset (https://www.kave.cc/datasets) for step 1. Extract the dataset into same folder you cloned this repo in. You should then have an `Events-170301-2` folder. 

To pass this folder to the loader you'll have to use the path `/usr/app/mounted/Events-170301-2/` as the `<extracted KaVE dataset path>`.

Similarly, you're going to want to create the resulting sqlite database in your host machine's working directory so that it doesn't get erased once the docker container is done executing. To do this specify a path inside `/usr/app/mounted/` like `/usr/app/mounted/activity-mining-docker.db`. You will notice a new file `activity-mining-docker.db` will be created which you'll want to use for subsequent steps. 


**For all commands documented below prefix `docker run --volume <your local working dir>:/usr/app/mounted aianta/<image>` to the command arguments described.**

A full example command running the loader to injest KaVE event data into a sqlite database file is provided below: 

`docker run --volume //c/Users/aiant/phdspace/classes/663_software_analytics/activity-mining:/usr/app/mounted aianta/activity-mining-loader 0 /usr/app/mounted/Events-170301-2/ /usr/app/mounted/docker-db 0 0`

Valid `<sequncer>` values:
* `TimeSensitive5`
* `RelativeTimeSensitive`
* `OneToOne`

## Loader
**Docker Hub Image:** `aianta/activity-mining-loader`

Iterates through the KaVE events dataset and inserts all events into a sqlite database. Database is created if it doesn't exist.

Command:

`<extracted KaVE dataset path> <database path> <starting archive> <starting record>`

Example:

`/usr/app/mounted/Events-170301-2/ /usr/app/mounted//usr/app/mounted/activity-mining.db 0 0`

`<starting archive>` Skips loading events from archives before `<starting archive>`. First archive is `1`. Used to restart an interrupted loading process.

`<starting record>` Skips loading events before `<starting record>` in `<starting archive>`. First record is `1`. Used to restart an interrupted loading process.

## Sequence Builder
**Docker Hub Image**:`aianta/activity-mining-build-sequences`

Takes events from `EVENTS` table in database and builds activity interval sequences. Then produces their string representation using a specified sequncer. Results are inserted into `SEQUENCES` table in the database.

Command:

`<database path> <sequencer> <skip>` 

Example:

`/usr/app/mounted/activity-mining.db TimeSensitive5 0`

`<skip>` Skips encoding first `<skip>` number of activity interval sequences into sequence strings. Used to restart interrupted sequencing process. 

## Sequence Miner
**Docker Hub Image**:`aianta/activity-mining-miner`

Has two modes `mine` and `export`. 

### In `mine` mode
Takes sequences produced by a given sequencer from the `SEQUENCES` table in the database and finds frequent sub-sequences using MG-FSM. Results are inserted into `FREQUENT_SUBSEQUENCES` table. 

Command:

`mine <database path> <mgfsm input dir> <mgfsm output dir> <java home> <output file name> <sequencer> <support> <gamma> <lambda>`

Example:

`mine /usr/app/mounted/activity-mining.db seqInput seqOutput "C:\Program Files\AdoptOpenJDK\jdk-8.0.212.03-hotspot" translatedFS TimeSensitive5 757 1 360`

`<mgfsm input/output dir>` organizes mgfsm inputs and outputs into these folders so you can keep track of which mining run was done with what parameters. Folders are created if they do not exist.

`<support> <gamma> <lambda>` MG-FSM mining parameters.

### In `export` mode

Takes mining results from `FREQUENT_SUBSEQUENCES` table with a given `executionId` (same as the folder name generated in the mining mode), and prints produces a `<executionId>.csv` file containing the FSS in a the correct format for SGT embedding.

Command:

`export <executionId>`

Example:

`export d9729ac3-e36b-490a-81cc-55bc7d84f0b1`

## Compute Clusters
**Docker Hub Image**:`aianta/activity-mining-compute-clusters`

Has two modes `cluster` and `export`

### In `cluster` mode
Reads in a file of FSS and a file of FSS embeddings and runs K-medoids with all values of K between the ranges of `<K_MIN>` and `<K_MAX>`. For each value of K runs the clustering `<EVALUATION_ITERATIONS>` number of times and reports the mean silhouette index achieved over those iterations in a record that can be found in the `CLUSTERING_RECORDS` table. Inserts individual cluster data points in the `CLUSTER_DATA` table.

Command:

`cluster <database path> <embedding file path> <sequence file path> <execution/mining id> <kappa value used for embedding> <K_MIN> <K_MAX> <EVALUATION_ITERATIONS>`

Example:

`cluster /usr/app/mounted/activity-mining.db 1d2923ae-5ae2-40cc-88ef-a5d3d5c0ae31_embeddings_k_5.csv 1d2923ae-5ae2-40cc-88ef-a5d3d5c0ae31.csv 1d2923ae-5ae2-40cc-88ef-a5d3d5c0ae31 5 2 20 5`

### In `export` mode
Exports the cluster data points for a particular iteration (recall that clustering is done `<EVALUATION_ITERATIONS>` number of times) of a clustering id  (found in the clustering record in `CLUSTERING_RECORDS` table) to a `clustering-data-<clusteringId>.csv` file. 

This can be used to visualize the clustering.

Command: 

`export <clusteringId> <iteration>`

Example:

`export 77a5cba5-11f3-4c4d-bfa7-e3569a236b79 1`

