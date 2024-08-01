#!/bin/bash

# isFull
curl -X GET "8086/isFull"

# stressTest
curl -X GET "8086/stressTest?arg1=<int>&arg2=<int>"

# size
curl -X GET "8086/size"

# isEmpty
curl -X GET "8086/isEmpty"

# poll
curl -X GET "8086/poll"

# offer
curl -X POST -d "8086/offer?arg1=<Object>"

