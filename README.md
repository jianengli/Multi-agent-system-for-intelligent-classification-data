# Multi-agent-system-for-intelligent-classification-data
A multi-agent system based on the JADE framework. Faced with a data set in xls or csv format, it can intelligently classify data according to specific classification algorithm instructions, and return classification results and classification performance.

## Table of Contents

- [Background](#background)
- [Running result](#running-result)
- [UML](#uml)
- [Prerequisites](#prerequisites)
- [Installment](#installment) 
- [Maintainers](#maintainers)

## Background
In this system, we created 4 agents, data agents and 3 classification agents. The data agent sends, and the classification agent is used to receive the data set and execute the corresponding algorithm for classification.

The data agent can read csv or TXT files. We believe that the "training set" folder provides supplementary data. The xls format can be used for knn classification and Naive Bayes classification, and the csv format can be used for kmeans classification.

After the classification agent accepts the data stream (string), it converts the data into R statements, and uses Rserve to remotely execute R scripts, apply specific classification algorithms for data analysis, and return classification results on Netbeans.

The classification algorithms contains:
1. KNN
2. Kmeans
3. Naive Bayes

The classification results contains:
1. Confusion matrix
2. Accuracy
3. Precision
4. Recall (completeness) of a mode
5. The F1 Score
6. Graphical plots

## Running result
- [Running result](https://github.com/jianengli/Multi-agent-system-for-intelligent-classification-data/blob/master/GIF.gif)

## UML
- [How is the data transmitted and classified?](https://github.com/jianengli/Multi-agent-system-for-intelligent-classification-data/blob/master/UML.png)

## Prerequisites
Configure Netbeans
Connect Rserve

## Installment
First, run R code:
```
library(Rserve)
Rserve()
```
Second, run the Java code and follow steps on Running result part.

## Maintainers
[@Jianeng](https://github.com/jianengli).
