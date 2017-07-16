import cv2
import numpy as np
import sys
import os
import matplotlib.pyplot as plt

img_path = os.path.abspath(sys.argv[1])

def edge_stage(img):
	"Process the image and returns an image with the edges of the input."
	grayscale = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
	median = cv2.medianBlur(grayscale, 5)
	edges = cv2.Canny(median, 100, 200)
	kernel = np.ones((2,2),np.uint8)
	dilated_img = cv2.dilate(edges, kernel, iterations=1)
	ret, threshold = cv2.threshold(dilated_img, 10, 255, cv2.THRESH_BINARY)
	return [grayscale, median, edges, dilated_img, threshold], ["grayscale", "median", "edges", "dilated_img", "threshold"]

def color_stage(img):
	"Performs color quantization and returns the image"
	height, width = img.shape[:2]
	downsample = cv2.resize(img, (width/4, height/4), interpolation = cv2.INTER_AREA)
	downsample_filtered = cv2.bilateralFilter(downsample, 9, 75, 75)
	upsample = cv2.resize(downsample_filtered, (width, height), interpolation = cv2.INTER_CUBIC)
	median = cv2.medianBlur(upsample, 5)
	quantized = (median/24)*24
	return [downsample, downsample_filtered, upsample, median, quantized], ["downsample", "downsample_filtered", "upsample", "median", "quantized"]

def recombine(edge_img, color_img):
	"Recombine the two images to generate the final toonified output"
	print(edge_img.shape, color_img.shape)
	toon_image = cv2.bitwise_and(color_img,color_img, mask = edge_img)
	return toon_image

if __name__=="__main__":
	input_img = cv2.imread(img_path, 1)
	print("The size of the input image is {}".format(input_img.shape))	
	edge_steps, edge_titles = edge_stage(input_img)
	color_steps, color_titles = color_stage(input_img)
	toon_img = recombine(edge_steps[-1], color_steps[-1])
	
	all_imgs = edge_steps + color_steps + [input_img, toon_img,]
	titles = edge_titles + color_titles + ["input_image", "Toon_image"]
 
	for i in range(len(all_imgs)):
		plt.subplot(3,5,i+1)
		plt.imshow(all_imgs[i])
		plt.title(titles[i])
	plt.show()
