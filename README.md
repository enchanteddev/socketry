# socketry
Packet structure
Actions (1 Byte): CALL, RESULT, ERROR, INIT, ACCEPT, PING, PONG

## Call
- FnID (Int) (1 Byte) max limit is 256 functions
- CallId (Int) (1 Byte) max limit is 256 calls in transit
- Arguments (n Bytes) JSON

## Result
- FnID (Int) (1 Byte) max limit is 256 functions
- CallId (Int) (1 Byte) max limit is 256 calls in transit
- Response (n Bytes) JSON

## Error
- FnID (Int) (1 Byte) max limit is 256 functions
- CallId (Int) (1 Byte) max limit is 256 calls in transit
- Error (n Bytes) String

## Init
- SocketsPerChannel (Int) (1 Byte) max limit is 256 sockets per channel
... repeated NumberOfChannels times

## Accept
NA

## Ping
NA

## Pong
NA