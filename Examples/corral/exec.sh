
for f in experiments/*.json; do
   originalfile=$f
    tmpfile=$(mktemp)
    cp --attributes-only --preserve $f $tmpfile
    cat $f | envsubst > $tmpfile && mv $tmpfile $f
done
echo "envsubst complete the config files are now ready - starting the execution"
mkdir runs
for f in experiments/q*.json; do
  echo "starting corral exectution with $f"
  ./corral_plus_tpch -config "$f"
  sleep 2
done
./corral_plus_tpch -config experiments/cleanup.json
