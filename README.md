# Socketry
Packet structure
Actions (1 Byte): CALL, RESULT, ERROR, INIT, ACCEPT, PING, PONG

## Call
- FnID (Int) (1 Byte) max limit is 256 functions
- CallId (Int) (1 Byte) max limit is 256 calls in transit
- Arguments (n Bytes) Arbitrary bytes

## Result
- FnID (Int) (1 Byte) max limit is 256 functions
- CallId (Int) (1 Byte) max limit is 256 calls in transit
- Response (n Bytes) Arbitrary bytes

## Error
- FnID (Int) (1 Byte) max limit is 256 functions
- CallId (Int) (1 Byte) max limit is 256 calls in transit
- Error (n Bytes) String

## Init
- SocketsPerChannel (Int) (1 Byte) max limit is 256 sockets per channel
... repeated NumberOfChannels times

## Accept
- Port numbers (2Byte per port) of each of the socket in the order of the socketsperchannel sent

So if socketsperchannel is 2,3 then total (2 + 3) * 2 Bytes = 10 Bytes

## Ping
NA

## Pong
NA