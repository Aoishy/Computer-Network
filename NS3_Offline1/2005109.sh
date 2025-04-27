nodes=(20 40 70 100) 
packets_per_sec=(100 200 300 400) 
node_speeds=(5 10 15 20) 
 
csv_file_name="results.csv" 
 
for n in "${nodes[@]}"; do 
    command="./ns3 run \"scratch/manet-routing-compare \ 
        --CSVfileName=${csv_file_name} \ 
        --protocol=AODV --flowMonitor=true \ 
        --nodeNumber=${n} --packet_Number=100 --nodeSpeed=5\"" 
    echo "Running: $command" 
    eval $command 
done 
 
for n in "${packets_per_sec[@]}"; do 
    command="./ns3 run \"scratch/manet-routing-compare \ 
        --CSVfileName=${csv_file_name} \ 
        --protocol=AODV --flowMonitor=true \ 
        --nodeNumber=20 --packet_Number=${n} --nodeSpeed=5\"" 
    echo "Running: $command" 
    eval $command 
done 
 
for n in "${node_speeds[@]}"; do 
    command="./ns3 run \"scratch/manet-routing-compare \ 
        --CSVfileName=${csv_file_name} \ 
        --protocol=AODV --flowMonitor=true \ 
        --nodeNumber=20 --packet_Number=100 --nodeSpeed=${n}\"" 
    echo "Running: $command" 
    eval $command 
done