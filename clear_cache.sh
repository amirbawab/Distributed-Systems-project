#!/bin/bash

echo ">> Removing Room RM cache"
rm -rf ./rmi/rm/data_room

echo ">> Removing Flight RM cache"
rm -rf ./rmi/rm/data_flight

echo ">> Removing Car RM cache"
rm -rf ./rmi/rm/data_car

echo ">> Removing MS RF cache"
rm -f ./rmi/midserver/RF_table

echo ">> Removing MS RMF cache"
rm -f ./rmi/midserver/RMF_table

echo ">> Removing MS TM cache"
rm -f ./rmi/midserver/TM_table
