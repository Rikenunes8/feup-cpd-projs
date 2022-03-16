import time
import numpy as np

def createMatrixes(nlines, ncols):
    print("Creating matrixes.", end="")
    matrixA = createMatrix(nlines, ncols, 'A')
    print(".", end="")
    matrixB = createMatrix(nlines, ncols, 'B')
    print(".", end="\t")
    matrixC = createMatrix(nlines, ncols, 'C')
    print("DONE!")
    return (matrixA, matrixB, matrixC)

def createMatrix(nlines, ncols, type):
    matrix = []

    for l in range(0, nlines):
        cols = []
        for _ in range(0, ncols):
            if type == 'A':
                cols.append(1.0)
            if type == 'B':
                cols.append(l + 1.0)
            if type == 'C':
                cols.append(0.0)
        matrix.append(cols)
        
    return matrix

def OnMult(nlines, ncols):
    print("\nOnMult Starting...")

    (matrixA, matrixB, matrixC) = createMatrixes(nlines, ncols)

    print("Starting calculations ({}x{})".format(nlines, ncols))  
    start = time.time()
  

    for l in range(0, nlines):
        for c in range(0, ncols):
            for k in range(0, nlines):
                matrixC[l][c] += matrixA[l][k] * matrixB[k][c]

    end = time.time()

    print("time taken: ", end - start)
    print("Sample of result matrix:\n", matrixC[0][:10])


def OnMultFaster(nlines, ncols):
    print("\nOnMult Starting...")

    (matrixA, matrixB, matrixC) = createMatrixes(nlines, ncols)

    print("Starting calculations ({}x{})".format(nlines, ncols))  
    start = time.time()
  

    for l in range(0, nlines):
        for c in range(0, ncols):
            temp = 0
            for k in range(0, nlines):
                temp += matrixA[l][k] * matrixB[k][c]
            matrixC[l][c] = temp

    end = time.time()

    print("time taken: ", end - start)
    print("Sample of result matrix:\n", matrixC[0][:10])

def OnMultLine(nlines, ncols):

    print("\nOnMultLine Starting...")

    (matrixA, matrixB, matrixC) = createMatrixes(nlines, ncols)

    print("Starting calculations ({}x{})".format(nlines, ncols))  
    start = time.time()

    for l in range(0, nlines):
        for k in range(0, nlines):
            for c in range(0, ncols):
                matrixC[l][c] += matrixA[l][k] * matrixB[k][c]

    end = time.time()

    print("time taken: ", end - start)
    print("Sample of result matrix:\n", matrixC[0][:10])


def OnMultBlock(nlines, ncols):

    print("\OnMultBlock Starting...")

    (matrixA, matrixB, matrixC) = createMatrixes(nlines, ncols)

    print("Starting calculations ({}x{})".format(nlines, ncols))  
    start = time.time()



    



    for l in range(0, nlines):
        for c in range(0, ncols):
            for k in range(0, nlines):
                matrixC[l][c] += matrixA[l][k] * matrixB[k][c]







    end = time.time()

    print("time taken: ", end - start)
    print("Sample of result matrix:\n", matrixC[0][:10])




OnMult(512, 512)
OnMultLine(512, 512)

