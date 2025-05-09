# wangblows defender HATES this and it will FUCK YOUR CPU over it even with the sleep call

from time import sleep

while True:
    sleep(0.1)
    try:
        with open("../../starsector-core/starsector.log", "r") as file:
            for line in reversed(file.readlines()):
                line = line.split()
                if line[3] == "data.scripts.util.ReflectHandles":
                    print(line[4]+line[5].rstrip("\n"), end="\r")
                    break
    except (KeyboardInterrupt, IndexError) as e:
        if type(e) == IndexError:
            continue
        break
