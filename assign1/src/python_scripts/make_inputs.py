reps = 3

since1 = 600
to1 = 3000
step1 = 400

since2 = 4096
to2 = 10240
step2 = 2048

blocks = [128, 256, 512]


f = open("inputs.txt", "w")

for i in range(since1, to1+1, step1):
  for _ in range(reps):
    f.write("1\n")
    f.write(str(i)+"\n")

for i in range(since1, to1+1, step1):
  for _ in range(reps):
    f.write("2\n")
    f.write(str(i)+"\n")



for i in range(since2, to2+1, step2):
  for _ in range(reps):
    f.write("2\n")
    f.write(str(i)+"\n")

for b in blocks:
  for i in range(since2, to2+1, step2):
    for _ in range(reps):
      f.write("3\n")
      f.write(str(i)+"\n")
      f.write(str(b)+"\n")

f.write("0")

f.close()