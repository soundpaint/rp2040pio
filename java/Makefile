# Makefile for Java tree of RP2040 PIO emulator
#
# Copyright (C) 2021 Jürgen Reuter
#
# This program is free software; you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 2 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; if not, write to the Free Software
# Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
#
# For updates and more info or contacting the author, visit:
# <https://github.com/soundpaint/rp2040pio>
#
# Author's web site: www.juergen-reuter.de

ROOT_DIR=..
include ../defs.mak

COMPILE_CLASSPATH=$(JAVA_DIR):$(BUILD_DIR)
RUN_CLASSPATH=.

JAVA_SRC=$(wildcard $(JAVA_DIR)/org/soundpaint/rp2040pio/*.java)
JAVA_OBJ=$(patsubst $(JAVA_DIR)/%.java,$(BUILD_DIR)/%.class,$(JAVA_SRC))

IMAGES_SRC_DIR=$(JAVA_DIR)/images
IMAGES_OBJ_DIR=$(BUILD_DIR)/images
IMAGES_SRC=$(wildcard $(IMAGES_SRC_DIR)/*.png)
IMAGES_OBJ=$(patsubst $(IMAGES_SRC_DIR)/%,$(IMAGES_OBJ_DIR)/%,$(IMAGES_SRC))

EXAMPLES_SRC_DIR=$(JAVA_DIR)/examples
EXAMPLES_OBJ_DIR=$(BUILD_DIR)/examples
EXAMPLES_SRC=$(wildcard $(EXAMPLES_SRC_DIR)/*.hex)
EXAMPLES_OBJ=$(patsubst $(EXAMPLES_SRC_DIR)/%,$(EXAMPLES_OBJ_DIR)/%,$(EXAMPLES_SRC))

JAR_OBJ=$(JAR_DIR)/rp2040pio.jar

all: obj jar

obj: $(BUILD_DIR) $(JAVA_OBJ) \
	$(IMAGES_OBJ_DIR) $(IMAGES_OBJ) \
	$(EXAMPLES_OBJ_DIR) $(EXAMPLES_OBJ)

$(BUILD_DIR):
	mkdir -p $@

$(IMAGES_OBJ_DIR):
	echo BUILD_IMAGES_DIR=$(IMAGES_OBJ_DIR)
	mkdir -p $@

$(EXAMPLES_OBJ_DIR):
	echo BUILD_EXAMPLES_DIR=$(EXAMPLES_OBJ_DIR)
	mkdir -p $@

$(JAR_DIR):
	mkdir -p $@

jar: $(JAR_OBJ)

$(JAR_OBJ): $(JAR_DIR) $(JAVA_OBJ)
	cd $(BUILD_DIR); \
	jar -0cvfm $(JAR_DIR)/rp2040pio.jar $(JAVA_DIR)/META-INF/MANIFEST.MF .

$(IMAGES_OBJ_DIR)/%.png: $(IMAGES_SRC_DIR)/%.png
	cp -pf $< $@

$(EXAMPLES_OBJ_DIR)/%.hex: $(EXAMPLES_SRC_DIR)/%.hex
	cp -pf $< $@

depend:

tags:
	etags *.java

run: all
	cd $(JAR_DIR) ; java -jar $(JAR_OBJ)
	#cd $(BUILD_DIR) ; java -ea -cp $(RUN_CLASSPATH) org.soundpaint.rp2040pio.Main

objclean:
	- rm -rf $(BUILD_DIR)

jarclean:
	- rm -rf $(JAR_DIR)

bkpclean:
	- find $(JAVA_DIR) -name \*~ -exec /bin/rm -f {} \; -print

coreclean:
	rm -f core core.* vgcore.*

clean: objclean jarclean

distclean: clean bkpclean

.SUFFIXES: $(SUFFIXES) .java .class

$(BUILD_DIR)%class: $(JAVA_DIR)%java
	javac -Xlint:all -Xdiags:verbose -d $(BUILD_DIR) -cp $(COMPILE_CLASSPATH) $<

#  Local Variables:
#    coding:utf-8
#    mode:Makefile
#  End:
