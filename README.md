# YouTube View Prediction Project #

## Overview ##

- This project focuses on predicting the number of views for YouTube content based on channel statistics and trending topics. It combines data scraping and machine learning to analyze and estimate video performance.

## The system is divided into two main components: ##

### 1. YouTube Scraping Tool `Java` using `Yt-dlp` and `Youtube API V3`###

- A Java-based tool that collects and processes data from YouTube.

#### Features ####
- Users input a content topic (e.g., `Resident Evil 9`, `Food Review`, `Travel France`)
The tool scrapes relevant channel and video data

Extracts and converts data into structured features for prediction

### Feature	Description

    x1	Channel follower count
    x2	Epoch (time-related feature)
    x3	Total number of videos
    x4	Average views
    x5	Average comments
    x6	Average likes
    x7	Average video duration
    x8	Content freshness timestamp
    x9	Avg views (last 10 videos)
    x10	Avg likes (last 10 videos)
    x11	Avg comments (last 10 videos)
    x12	Avg duration (last 10 videos)
    x13	Upload frequency
    x14	Channel verification status
### 2. AI Prediction Model `Python`

- #### Feature : A machine learning module that predicts video views based on collected data.

- #### Technologies Used
        Pandas: Used for reading and processing .csv data files
        NumPy: Supports mathematical operations and matrix computations
        Scikit-learn: Provides machine learning models and utilities
        Matplotlib: Used for data visualization and plotting results
- #### Concepts & Knowledge Applied
        Linear Regression: Fundamental algorithm for predicting continuous values
        Normal Equation: Analytical approach to find optimal parameters in linear regression
        Regularization Techniques: Methods (e.g., Ridge, Lasso) to prevent overfitting
        Gradient-based Optimization: Techniques such as Gradient Descent for model training

- #### Workflow

        User inputs a content topic
        Java tool scrapes and extracts features
        Data is saved to CSV
        Python model processes data
        Model predicts expected view count

### Goal

To help content creators estimate potential video performance and make data-driven decisions when choosing topics.
