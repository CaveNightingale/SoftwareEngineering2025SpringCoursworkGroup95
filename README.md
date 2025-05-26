# Software Engineering 2025 Spring Coursework Group 95

## How to build

**Note: Although the source code is portable, the built artifacts are not. If you build in on Linux, your executable will not run on Windows, and vice versa. We may fix this in the future, but not early stages of development.**

Require JDK 21 or later. If you have multiple JDKs installed, you can set the `PATH` environment variable to the JDK you want to use. For example,

```bash
PATH=/usr/lib/jvm/java-21-openjdk/bin:$PATH java -version
```

Run the following command in the root directory of the project:

```bash
./gradlew build
```

The built artifacts will be in the `app/build` directory. If you want to distribute the application, you can use the `app/build/distributions/app.zip` directory. Unzipping it will give you two directory: `bin` and `lib`. Use the `app` script in the `bin` directory to run the application.

## How to run

```bash
./gradlew run
```

## How to test

```bash
./gradlew test
./gradlew bench
```

## How to use
You can click the `Help` button on the top right corner of the main page or `What is this?` hyperlink on the top right corner of the decryption page see the manual.

Or you can find it [here](app/src/main/resources/io/github/software/coursework/gui/_help.md). But remember that since we use base64 url, **Github will not render the images in that file and you will have to download the file**.

## Task Assignment

Our team is composed of 6 members, and divided into 3 sub-teams.
- **Team 1**: Huabin Yuan, Peidong Jia
  - Data Storage
  - ML Model GUI
  - Code Review
- **Team 2**: Xingzhou Zeng, Yingjie Huang
  - ML Model Dataset Preparation
  - ML Model Algorithm Implementation
- **Team 3**: Langdeng Tang, Zhongyuan Xu 
  - Miscellaneous GUI
  - Importing Data from Bank

Currently, the contribution of each member is as follows:

- **Huabin Yuan**:
    - Designed and implemented the data storage system to support efficient data management.
    - Developed data visualization features, providing users with insights into spending trends and categories.
    - Added auto-completion functionality to form fields to enhance user experience.

- **Peidong Jia**:
    - Collected and refined user stories to ensure clear and actionable requirements.
    - Revised and improved parts of the user interface for better usability and aesthetics.
    - Authored the user manual and conducted comprehensive application testing.

- **Yingjie Huang**:
    - Prepared and cleaned the dataset for the machine learning model to ensure data accuracy.
    - Applied data synthesis techniques to expand the variety of model training data.
    - Tested and fine-tuned the machine learning model to ensure reliable predictions.

- **Xingzhou Zeng**:
    - Designed and implemented algorithms for budgeting and savings analysis, providing personalized recommendations.
    - Developed logic for transaction classification and integrated it into the machine learning module.
    - Completed the core implementation of the machine learning model and ensured its functionality.

- **Langdeng Tang**:
    - Implemented the data importing feature to process bank-provided CSV files with error handling.
    - Developed real-time input validation for form fields, providing immediate feedback to users on errors.
    - Developed the transaction search functionality, allowing users to filter transactions by title.

- **Zhongyuan Xu**:
    - Added Markdown support for the user interface, enabling users to view and format content effectively.
    - Assisted in testing and debugging the application to ensure a smooth user experience.
