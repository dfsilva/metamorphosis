# Metamorphosis
> Metamorphosis allows you to connect on nats topics, create agents to transform the data and make decisions using Groovy Script.

[![Contributors][contributors-shield]][contributors-url]
[![Forks][forks-shield]][forks-url]
[![Stargazers][stars-shield]][stars-url]
[![Issues][issues-shield]][issues-url]
[![MIT License][license-shield]][license-url]
[![LinkedIn][linkedin-shield]][linkedin-url]

##Features

* Transform data from a topic using dynamic script
* Make decisions where the message will be delivered after the transformation using a second dynamic script
* Configure if the messages will be processed ordered or not.

<table border="0">
 <tr>
    <td><img src="images/screen1.png"/></td>
    <td><img src="images/screen2.png"/></td>
 </tr>
</table>

## Running

> You need to have the latest version of [Docker](https://www.docker.com/) installed to run this example

### Linux/Unix

One node:
```sh
./build-container-image.sh
./run-all.sh
```
Access [http://localhost:8081/](http://localhost:8081/)

Stop
```sh
./stop-all.sh
```
Multiple nodes:

```sh
./build-container-image.sh
./run-data.sh
./run-node0.sh
./run-node1.sh
./run-noden.sh
```

Access [http://localhost:8081/](http://localhost:8081/) and [http://localhost:8082/](http://localhost:8082/)

Stop

```sh
./stop-data.sh
docker rm -f node00
docker rm -f node01
```

### Windows

One node:

```sh
build-container-image.bat
run-all.bat
```
Access [http://localhost:8081/](http://localhost:8081/)

Stop
```sh
stop-all.bat
```

Multiple nodes:
```sh
build-container-image.bat
run-data.bat
run-node0.bat
run-node1.bat
run-noden.bat
```
Access [http://localhost:8081/](http://localhost:8081/) and [http://localhost:8082/](http://localhost:8082/)

Stop
```sh
./stop-data.sh
docker rm -f node00
docker rm -f node01
```

## Release History

* 0.0.1
    * Work in progress

## Meta

Diego Silva – [@diegosinf](https://twitter.com/diegosinf) – diego@diegosilva.com.br

Distributed under the MIT license. See ``LICENSE`` for more information.

[https://github.com/dfsilva/metamorphosis](https://github.com/dfsilva/metamorphosis/)

## Contributing

1. Fork it (<https://github.com/yourname/yourproject/fork>)
2. Create your feature branch (`git checkout -b feature/fooBar`)
3. Commit your changes (`git commit -am 'Add some fooBar'`)
4. Push to the branch (`git push origin feature/fooBar`)
5. Create a new Pull Request


[contributors-shield]: https://img.shields.io/github/contributors/dfsilva/metamorphosis.svg?style=for-the-badge
[contributors-url]: https://github.com/dfsilva/metamorphosis/graphs/contributors
[forks-shield]: https://img.shields.io/github/forks/dfsilva/metamorphosis.svg?style=for-the-badge
[forks-url]: https://github.com/dfsilva/metamorphosis/network/members
[stars-shield]: https://img.shields.io/github/stars/dfsilva/metamorphosis.svg?style=for-the-badge
[stars-url]: https://github.com/dfsilva/metamorphosis/stargazers
[issues-shield]: https://img.shields.io/github/issues/dfsilva/metamorphosis.svg?style=for-the-badge
[issues-url]: https://github.com/dfsilva/metamorphosis/issues
[license-shield]: https://img.shields.io/github/license/othneildrew/Best-README-Template.svg?style=for-the-badge
[license-url]: https://github.com/othneildrew/Best-README-Template/blob/master/LICENSE.txt
[linkedin-shield]: https://img.shields.io/badge/-LinkedIn-black.svg?style=for-the-badge&logo=linkedin&colorB=555
[linkedin-url]: https://www.linkedin.com/in/dsilva82
   