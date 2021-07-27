# MonomeGridToBitwig
Basic Processing/Java sketch which demonstrates one way to implement the [Monome Grid](https://monome.org/) as a clip launching controller for [Bitwig](https://www.bitwig.com/) through [Moss OSC Script](http://www.mossgrabers.de/Software/Bitwig/Bitwig.html)



_probably can be optimised, but until OSC is natively implemented in BitWig, this works..._

The sketch is setup to send on port 8005  ```int bitwigPort = 8005;``` . It is necesrary to set that as receiving port on the Moss OSC settings inside Bitwig preferences. 

I included a Mac and Win prebuild standalone for convience. On OS X , you need Java installed to run that. 


Libraries: OscP5, NetP5 & Monome for Processing.
