master
[![Tests](https://github.com/OGSegu/BasicBlockChain/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/OGSegu/BasicBlockChain/actions/workflows/gradle.yml)  
dev
[![Tests](https://github.com/OGSegu/BasicBlockChain/actions/workflows/gradle.yml/badge.svg?branch=dev)](https://github.com/OGSegu/BasicBlockChain/actions/workflows/gradle.yml)
# Basic BlockChain
***

![blockchain](blockchain-ai.png)  
<sub><sup>*Картинка сгенерирована нейронной сетью</sub></sup>

Базовая реализация блокчейна из 3 нод.

## Как запускать
***
1. Собрать докер образ  
```
docker build -t blockchain_node:latest .
```
2. Запустить через docker-compose  
```
docker-compose up
```

## Демонстрация работы
***

![Пример работы](work-example.png)

Между нодами происходит обмен блоками