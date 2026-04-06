import pandas as pd
import matplotlib.pyplot as plt 
import numpy as np
from  sklearn.metrics import mean_squared_error
from sklearn.linear_model import SGDRegressor, Ridge
from sklearn.preprocessing import StandardScaler

def readDataFile(filePath:str) -> pd.DataFrame:
    dataFile = pd.read_csv(filePath)

    return dataFile

def getXandY(dataFile: pd.DataFrame) -> tuple[np.ndarray, np.ndarray]:
    X  = dataFile[["epoch", "followers", "video_count", "avg10View", "avg10Like", "avg10Comment", "avg10Duration", "frequency", "isVerify", "avgView", "avgLikes", "avgComments", "avgDuration"]].values
    Y = dataFile[["view"]].values.flatten()

    return X, Y

def drawHistoryE(historyE: np.ndarray, interation: int) -> None :
    plt.plot(range(len(historyE)), historyE, color='blue', label='CostFunction E')
    plt.xlabel("Interation")
    plt.ylabel("E")
    plt.title("E and Interation")
    plt.grid(True)
    plt.legend()
    

def drawHxAndY(hx: np.ndarray, Y: np.ndarray) -> None:
    plt.scatter(range(len(hx)), hx, color='blue', label='Hx')
    plt.plot(range(len(Y)), Y, color='red', label="Y")
    plt.xlabel("Position")
    plt.ylabel("Value")
    plt.title("Hx and Y")
    plt.grid(True)
    plt.legend()


def drawTest(hxTest: np.ndarray, YTest: np.ndarray )-> None:
    plt.scatter(range(len(hxTest)), hxTest, color='blue', label='HxTEst')
    plt.plot(range(len(YTest)), YTest, color='red', label="YTest")
    plt.xlabel("Position")
    plt.ylabel("Value")
    plt.title("HxTest and YTest")
    plt.grid(True)
    plt.legend()

def drawMatLab(historyE: np.ndarray, interation: int, hx: np.ndarray, Y: np.ndarray, hxTest: np.ndarray, YTest: np.ndarray):

    fig1 = plt.figure(1)
    drawHistoryE(historyE, interation)

    fig2 = plt.figure(2)
    drawHxAndY(hx, Y)

    fig3 = plt.figure(3)
    drawTest(hxTest, YTest)
    

    plt.show()  



if __name__ == "__main__":
    filePath: str = "Valorant.csv"
    interation: int = 1000
    learningRate: int = 0.0000001
    regularization: float = 0.01

    dataFile: pd.DataFrame = readDataFile(filePath)


    X, Y = getXandY(dataFile)

    scalerX = StandardScaler()
    X = scalerX.fit_transform(X)


    scalerY = StandardScaler()
    Y = scalerY.fit_transform(Y.reshape(-1,1)).flatten()

    n = len(X)
    XTrain = X[:n-10]
    YTrain = Y[:n-10]

    XTest = X[n-10:]
    YTest = Y[n-10:]

    linearModel = SGDRegressor(
        penalty='l2',
        alpha=regularization,
        max_iter=1,
        learning_rate='constant',
        eta0=learningRate,
        warm_start=True
    )

    historyE = np.zeros(interation)
    finalTheta = np.zeros(X.shape[1])
    finalHx = np.zeros(X.shape[0])
    for inter in range(interation):
        linearModel.partial_fit(XTrain, YTrain) 
        # linearModel.fit(XTrain, YTrain)
        hx = linearModel.predict(XTrain)
        E = mean_squared_error(YTrain, hx)
        historyE[inter] = E

        print(E)

        if inter == interation-1:
            finalTheta = linearModel.coef_
            finalHx = hx

    print(f"The finnal theta is : {finalTheta}")
    print(f"The finnal E is : {historyE[interation-1]}")


     

    # linearModel.fit(XTest, YTest)
    hxTest  = linearModel.predict(XTest)
    ETEst = mean_squared_error(YTest, hxTest)


    print(f"Test E is : {ETEst}")

    drawMatLab(historyE, interation, hx, YTrain, hxTest, YTest)

