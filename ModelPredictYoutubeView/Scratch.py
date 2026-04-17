import numpy as np
import pandas as pd 
import matplotlib.pyplot as plt
from sklearn.preprocessing import StandardScaler

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

def calculateCostFunctionWithLambda(X: np.ndarray, theta: np.ndarray, 
                                    Y: np.ndarray, lambdaNumber) -> float:
    m = len(X)
    hx = calculateHx(X, theta)
    regularizationComponent = lambdaNumber * np.sum(theta[1:]**2)
    cost = (np.sum((hx - Y.flatten())**2)  + regularizationComponent ) / (2 * m) 
    return cost

def calculateCostFunction(X: np.ndarray, theta: np.ndarray, Y: np.ndarray) -> float:
    m = len(X)
    cost = np.sum((calculateHx(X, theta) - Y.flatten())**2) / (2 * m)
    return cost

def calculateTheta(X: np.ndarray, Y: np.ndarray, theta: np.ndarray, 
                   iteration: int, learningRate: float, lambdaNumber):
    m = len(X)
    historyELambda = np.zeros(iteration)
    historyE = np.zeros(iteration)
    for i in range(iteration):
        hx = calculateHx(X, theta)
        gradientE = X.T.dot(hx - Y.flatten()) / m
        gradientE[1:] += (lambdaNumber / m) * theta[1:]
        theta -= learningRate * gradientE
        historyE[i] = calculateCostFunction(X, theta, Y)
        historyELambda[i] = calculateCostFunctionWithLambda(X, theta, Y, lambdaNumber)
        print(f"{i}/{iteration} : {historyE[i]}")
    return theta, historyELambda, historyE

def calculateR2(Y: np.ndarray, hx: np.ndarray) -> float:
    Y = Y.flatten()
    hx = hx.flatten()
    
    y_mean = np.mean(Y)
    
    ss_res = np.sum((Y - hx) ** 2)
    
    ss_tot = np.sum((Y - y_mean) ** 2)
    
    if ss_tot == 0:
        return 0.0
    
    r2 = 1 - (ss_res / ss_tot)
    
    return r2

def drawHistoryE(historyE: np.ndarray, iteration: int) -> None :
    plt.plot(range(len(historyE)), historyE, color='blue', label='CostFunction E')
    plt.xlabel("iteration")
    plt.ylabel("E")
    plt.title("E and iteration")
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

def drawFeatureImportance(theta: np.ndarray, feature_names: list[str]) -> None:
    importance = np.abs(theta[1:])
    
    sorted_idx = np.argsort(importance)[::-1]
    sorted_importance = importance[sorted_idx]
    sorted_features = np.array(feature_names)[sorted_idx]

    plt.barh(range(len(sorted_importance)), sorted_importance, color='green')
    plt.yticks(range(len(sorted_importance)), sorted_features)
    plt.xlabel("Importance (|theta|)")
    plt.ylabel("Features")
    plt.title("Feature Importance (Horizontal)")
    plt.gca().invert_yaxis() 
    plt.grid(True)

def drawViewDistribution(Y: np.ndarray) -> None:
    mean_view = np.mean(Y)
    
    plt.scatter(range(len(Y)), Y, color='blue', label='View')


    plt.axhline(y=mean_view, color='red', label=f'Mean = {mean_view:.2f}')
    
    plt.xlabel("Sample Index")
    plt.ylabel("View")
    plt.title("View Distribution vs Mean")
    plt.legend()
    plt.grid(True)

def drawMatLab(historyE: np.ndarray, iteration: int, hx: np.ndarray, Y: np.ndarray, hxTest: np.ndarray, YTest: np.ndarray, theta: np.ndarray, featureName : list[str]):
    fig1 = plt.figure(1)
    drawHistoryE(historyE, iteration)
    fig2 = plt.figure(2)
    drawHxAndY(hx, Y)
    fig3 = plt.figure(3)
    drawTest(hxTest, YTest)

    fig4 = plt.figure(4)
    drawFeatureImportance(theta, featureName)

    fig5 = plt.figure(5)
    drawViewDistribution(Y)


    plt.show()  

if __name__ == "__main__":
    dataFile = readDataFile("Valorant.csv")
    iteration = 2000000
    learningRate = 0.000001
    lambdaNumber = 0.01

    X: np.ndarray
    Y: np.ndarray
    X, Y = getXandY(dataFile)

    scalerX = StandardScaler()
    X = scalerX.fit_transform(X)

    X = addBias1(X)

    n:int = len(X)
    XTrain = X[:n-50]
    YTrain = Y[:n-50]

    XTest = X[n-50:]
    YTest = Y[n-50:]

    theta = addRandomTheta(X)

    print(f"Initial Theta : {theta}")


    theta, historyELambda, historyE = calculateTheta(XTrain, YTrain, theta, iteration, learningRate, lambdaNumber)
    hxFinal = calculateHx(XTrain, theta)
    hxTest = calculateHx(XTest, theta)

    E = calculateCostFunctionWithLambda(XTrain, theta, YTrain, lambdaNumber)

    print(f"List final theta : {theta}")
    print(f"The final E with lambad = {historyE[-1]}")
    print(f"The final E = {E}")

    r2_train = calculateR2(YTrain, hxFinal)
    r2_test = calculateR2(YTest, hxTest)

    print(f"R2 Train: {r2_train*100}")
    print(f"R2 Test: {r2_test*100}")

    feature_names = ["epoch", "followers", "video_count", "avg10View", 
                 "avg10Like", "avg10Comment", "frequency", 
                 "predict_view", "predict_like", "predict_comment"]


    drawMatLab(historyE, iteration, hxFinal, YTrain, hxTest, YTest, theta, feature_names)