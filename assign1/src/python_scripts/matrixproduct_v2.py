import time

def OnMult(m_ar, m_br):

	pha = [[1.0 for j in range(m_ar)] for j in range(m_br)]
	phb = [[i+1 for j in range(m_br)] for i in range(m_ar)]
	phc = [[0 for j in range(m_ar)] for j in range(m_br)]
	
	Time1 = time.time()

	for i in range(m_ar):
		for j in range(m_br):
			for k in range(m_ar):
				phc[i][j] += pha[i][k] * phb[k][j]

	Time2 = time.time()
	print("Time: {} seconds".format(Time2-Time1))

	# display 10 elements of the result matrix tto verify correctness
	print("Result matrix:")
	print(phc[0][:10])

def OnMultLine(m_ar, m_br):
  pha = [[1.0 for j in range(m_ar)] for j in range(m_br)]
  phb = [[i+1 for j in range(m_br)] for i in range(m_ar)]
  phc = [[0 for j in range(m_ar)] for j in range(m_br)]
	
  Time1 = time.time()

  for i in range(m_ar):
    for k in range(m_ar):
      for j in range(m_br):
        phc[i][j] += pha[i][k] * phb[k][j]


  Time2 = time.time()
  print("Time: {} seconds".format(Time2-Time1))

	# display 10 elements of the result matrix tto verify correctness
  print("Result matrix:")
  print(phc[0][:10])
  return

def OnMultBlock(m_ar, m_br):
  return

def main():
  while True:
    print("\n0. Exit")
    print("1. Multiplication")
    print("2. Line Multiplication")
    print("3. Block Multiplication")
    op = int(input("Selection?: "))

    if op == 0:
      break

    n = int(input("Dimensions: lins=cols ? "))
    if op == 1:
      OnMult(n, n)
    elif op == 2:
      OnMultLine(n, n)
    elif op == 3:
      blockSize = int(input("Block Size? "))
      OnMultBlock(n, n)
    
main()