FROM python:3.12

ADD requirements.txt /
ADD processor /processor

RUN pip3 install --upgrade pip
RUN pip3 install -r requirements.txt

USER nobody

CMD ["python3", "-m", "processor.process"]
