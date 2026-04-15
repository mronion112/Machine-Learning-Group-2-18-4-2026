import numpy as np
import pandas as pd 
import matplotlib.pyplot as plt
from  sklearn.metrics import mean_squared_error
from sklearn.linear_model import SGDRegressor, Ridge
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import r2_score

def readDataFile(filePath:str) -> pd.DataFrame:
    dataFile = pd.read_csv("data/"+filePath)
    return dataFile

def getXandY(dataFile: pd.DataFrame) -> tuple[np.ndarray, np.ndarray]:
    X  = dataFile[["epoch", "followers", "video_count", "avg10View", "avg10Like", "avg10Comment", "frequency", "predict_view", "predict_like", "predict_comment"]].values
    Y = dataFile[["view"]].values.flatten()
    return X, Y

def addBias1(X: np.ndarray) -> np.ndarray:
    X = np.c_[np.ones(X.shape[0]), X]
    return X

def addRandomTheta(X: np.ndarray) -> np.ndarray:
    theta = np.random.rand(X.shape[1])
    return theta

def calculateHx(X: np.ndarray, theta: np.ndarray) -> np.ndarray:
    hx = np.dot(X, theta)
    return hx

def calculateCostFunction(X: np.ndarray, theta: np.ndarray, Y: np.ndarray, lambdaNumber) -> float:
    m = len(X)
    hx = calculateHx(X, theta)
    regularizationComponent = lambdaNumber * np.sum(theta[1:]**2)
    cost = (np.sum((hx - Y.flatten())**2)  + regularizationComponent ) / (2 * m) 
    return cost

def calculateTheta(X: np.ndarray, Y: np.ndarray, theta: np.ndarray, interation: int, learningRate: float, lambdaNumber):
    m = len(X)
    historyE = np.zeros(interation)
    for i in range(interation):
        hx = calculateHx(X, theta)
        gradientE = X.T.dot(hx - Y.flatten()) / m
        gradientE[1:] += (lambdaNumber / m) * theta[1:]
        theta -= learningRate * gradientE
        historyE[i] = calculateCostFunction(X, theta, Y, lambdaNumber)
    return theta, historyE

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
    dataFile = readDataFile("Valorant.csv")

    X: np.ndarray
    Y: np.ndarray
    X, Y = getXandY(dataFile)

    scalerX = StandardScaler()
    X = scalerX.fit_transform(X)

    X = addBias1(X)

    n = len(X)
    XTrain = X[:n-50]
    YTrain = Y[:n-50]

    XTest = X[n-50:]
    YTest = Y[n-50:]

    theta = addRandomTheta(X)

    print(f"Initial Theta : {theta}")

    interation = 10000
    learningRate = 0.01
    lambdaNumber = 0.01

    theta, historyE = calculateTheta(XTrain, YTrain, theta, interation, learningRate, lambdaNumber)
    hxFinal = calculateHx(XTrain, theta)
    hxTest = calculateHx(XTest, theta)

    print(f"List final theta : {theta}")
    print(f"The final costfunction E = {historyE[-1]}")

    r2_train = r2_score(YTrain, hxFinal)
    r2_test = r2_score(YTest, hxTest)

    print(f"R2 Train: {r2_train*100}")
    print(f"R2 Test: {r2_test*100}")

    drawMatLab(historyE, interation, hxFinal, YTrain, hxTest, YTest)