import pandas as pd
import matplotlib.pyplot as plt 
import numpy as np
from sklearn.metrics import mean_squared_error, r2_score
from sklearn.ensemble import RandomForestRegressor


def readDataFile(filePath: str) -> pd.DataFrame:
    return pd.read_csv("data/"+filePath)


def getXandY(dataFile: pd.DataFrame) -> tuple[np.ndarray, np.ndarray]:
    X  = dataFile[[
        "epoch", "followers", "video_count", 
        "avg10View", "avg10Like", "avg10Comment", 
        "frequency", "predict_view", 
        "predict_like", "predict_comment"
    ]].values

    Y = dataFile["view"].values
    return X, Y


def drawHxAndY(hx: np.ndarray, Y: np.ndarray) -> None:
    plt.scatter(range(len(hx)), hx, color='blue', label='Prediction')
    plt.plot(range(len(Y)), Y, color='red', label="Ground Truth")
    plt.xlabel("Index")
    plt.ylabel("Value")
    plt.title("Train Prediction vs Actual")
    plt.grid(True)
    plt.legend()


def drawTest(hxTest: np.ndarray, YTest: np.ndarray) -> None:
    plt.scatter(range(len(hxTest)), hxTest, color='blue', label='Prediction Test')
    plt.plot(range(len(YTest)), YTest, color='red', label="Y Test")
    plt.xlabel("Index")
    plt.ylabel("Value")
    plt.title("Test Prediction vs Actual")
    plt.grid(True)
    plt.legend()


if __name__ == "__main__":
    filePath: str = "Valorant.csv"

    dataFile = readDataFile(filePath)
    X, Y = getXandY(dataFile)

    n = len(X)
    XTrain = X[:n-50]
    YTrain = Y[:n-50]

    XTest = X[n-50:]
    YTest = Y[n-50:]

    model = RandomForestRegressor(
        n_estimators=100,      
        max_depth=None,        
        random_state=42,
        n_jobs=-1              
    )

    model.fit(XTrain, YTrain)
    hxTrain = model.predict(XTrain)
    hxTest = model.predict(XTest)

    train_mse = mean_squared_error(YTrain, hxTrain)
    test_mse = mean_squared_error(YTest, hxTest)

    r2_train = r2_score(YTrain, hxTrain)
    r2_test = r2_score(YTest, hxTest)

    print(f"Train MSE: {train_mse}")
    print(f"Test MSE: {test_mse}")
    print(f"R2 Train: {r2_train}")
    print(f"R2 Test: {r2_test}")

    plt.figure(1)
    drawHxAndY(hxTrain, YTrain)

    plt.figure(2)
    drawTest(hxTest, YTest)

    plt.show()