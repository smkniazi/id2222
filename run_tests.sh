./compile.sh

# Experiments for different delta
./run.sh -graph=graphs/3elt.graph -temp=1.0 -delta=0.9 -factor=1.0
./run.sh -graph=graphs/add20.graph -temp=1.0 -delta=0.9 -factor=1.0
./run.sh -graph=graphs/twitter.graph -temp=1.0 -delta=0.9 -factor=1.0
./run.sh -graph=graphs/facebook.graph -temp=1.0 -delta=0.9 -factor=1.0

./run.sh -graph=graphs/3elt.graph -temp=1.0 -delta=0.95 -factor=1.0
./run.sh -graph=graphs/add20.graph -temp=1.0 -delta=0.95 -factor=1.0
./run.sh -graph=graphs/twitter.graph -temp=1.0 -delta=0.95 -factor=1.0

./run.sh -graph=graphs/3elt.graph -temp=1.0 -delta=0.97 -factor=1.0
./run.sh -graph=graphs/add20.graph -temp=1.0 -delta=0.97 -factor=1.0
./run.sh -graph=graphs/twitter.graph -temp=1.0 -delta=0.97 -factor=1.0

./run.sh -graph=graphs/3elt.graph -temp=1.0 -delta=0.99 -factor=1.0
./run.sh -graph=graphs/add20.graph -temp=1.0 -delta=0.99 -factor=1.0
./run.sh -graph=graphs/twitter.graph -temp=1.0 -delta=0.99 -factor=1.0

# Experiments with different factors
./run.sh -graph=graphs/3elt.graph -temp=1.0 -delta=0.9 -factor=0.5
./run.sh -graph=graphs/add20.graph -temp=1.0 -delta=0.9 -factor=0.5
./run.sh -graph=graphs/twitter.graph -temp=1.0 -delta=0.9 -factor=0.5

./run.sh -graph=graphs/3elt.graph -temp=1.0 -delta=0.9 -factor=1.5
./run.sh -graph=graphs/add20.graph -temp=1.0 -delta=0.9 -factor=1.5
./run.sh -graph=graphs/twitter.graph -temp=1.0 -delta=0.9 -factor=1.5