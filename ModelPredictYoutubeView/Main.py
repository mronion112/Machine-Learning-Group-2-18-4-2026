import pandas as pd
import matplotlib.pyplot as plt 
import numpy as np
from  sklearn.metrics import mean_squared_error
from sklearn.linear_model import SGDRegressor, Ridge
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import r2_score


def readDataFile(filePath:str) -> pd.DataFrame:
    dataFile = pd.read_csv("data/"+filePath)

    return dataFile

# channel	epoch	followers	video_count	avg10View	avg10Like	avg10Comment	frequency	predict_view	predict_like	predict_comment	view

def getXandY(dataFile: pd.DataFrame) -> tuple[np.ndarray, np.ndarray]:
    X  = dataFile[["epoch", "followers", "video_count", "avg10View", "avg10Like", "avg10Comment", "frequency", "predict_view", "predict_like", "predict_comment"]].values
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
    interation: int = 10000
    learningRate: int = 0.000000001
    regularization: float = 0.001

    dataFile: pd.DataFrame = readDataFile(filePath)

    X, Y = getXandY(dataFile)
    scalerX = StandardScaler()
    X = scalerX.fit_transform(X)
    
    # scalerY = StandardScaler()
    # Y = scalerY.fit_transform(Y.reshape(-1,1)).flatten()

    n = len(X)
    XTrain = X[:n-50]
    YTrain = Y[:n-50]

    XTest = X[n-50:]
    YTest = Y[n-50:]

    linearModel = SGDRegressor(
        penalty='l2',
        alpha=regularization,
        max_iter=1,
        learning_rate='optimal',
        # eta0=learningRate,
        warm_start=True
    )

    historyE = np.zeros(interation)
    finalTheta = np.zeros(X.shape[1])
    hxFinal = np.zeros(X.shape[0])
    for inter in range(interation):
        linearModel.partial_fit(XTrain, YTrain) 
        # linearModel.fit(XTrain, YTrain)
        hx = linearModel.predict(XTrain)
        E = mean_squared_error(YTrain, hx)/2
        historyE[inter] = E

        print(f"{inter}/{interation} : {E}")

        if inter == interation-1:
            finalTheta = linearModel.coef_
            hxFinal = hx

    print(f"The final theta is : {finalTheta}")
    print(f"The final E is : {historyE[interation-1]}")

    hxTest  = linearModel.predict(XTest)
    ETEst = mean_squared_error(YTest, hxTest)

    print(f"Test E is : {ETEst}")

    r2_train = r2_score(YTrain, hxFinal)
    r2_test = r2_score(YTest, hxTest)

    print(f"R2 Train: {r2_train*100}")
    print(f"R2 Test: {r2_test*100}")

    drawMatLab(historyE, interation, hxFinal, YTrain, hxTest, YTest)

