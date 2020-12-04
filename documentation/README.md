# MedForms Form Fields Configuration

This app is designed to generate Android “View” objects based on the values given in a JSON file. Examples of all Views that can be generated are included in this document.

## Form Field Types

- [textView](#textView)
- [editText](#editText)
- [radioGroup](#radioGroup)
- [radioGroupRatings](#radioGroupRatings)
- [checkboxGroup](#checkboxGroup)
- [dropDownList](#dropDownList)
- [sectionBreak](#sectionBreak)

<!-- - [checkbox](#checkbox) -->
<!-- - [switch](#switch) -->
<!-- - [datePicker](#datePicker) -->
<!-- - [timePicker](#timePicker) -->

## textView

A textView simply displays text on the form. It is defined in the JSON file as:

```JSON
{
  "type": "textView",
  "text": "An example of a textView! This is what you would
           put a prompt in. textSize is set to 20",
  "textSize": 20
}
```

The textSize property will change the size of the font displayed within the application. This is what would be generated from that JSON object:

![textView](/documentation/images/textView.png)

## editText

The editText View has a description and a "box" where text can be entered. It is defined in the JSON file as:

```JSON
{
  "type": "editText",
  "description": "Here's an editText you can fill in the blank
                  with anything you want.",
  "mode": 2,
  "singleLine": true
}
```

Here is what it looks like:

![editText](/documentation/images/editText1.png)

The editText object has 3 properties:

- description – This is what is displayed as a prompt for the blank.
- mode
  - 1 – This puts a "hint" in the blank box. The hint is defined in the "description" property. It looks like this:
    ![editTextHint](/documentation/images/editTextHint.png)
  - 2 – This puts the text from the description property above the blank box:
    ![editText](/documentation/images/editText1.png)
- singleLine — The singleLine option can be set to true or false:

```JSON
    {
      "type": "editText",
      "text": "Here's another editText text with
               singleLine set to false",
      "mode": 2,
      "singleLine": false
    }
```

![singleLineFalse](/documentation/images/singlelinefalse.png)

## radioGroup

Only one option from the list can be selected at a time.

```JSON
    {
      "type": "radioGroup",
      "description": "radioGroup - only one option can be
                      chosen",
      "options": [
        "option 1",
        "option 2",
        "option 3"
      ]
    }
```

![radioGroup](/documentation/images/radioGroup.png)

There are 2 properties for radioGroup:

- description – The text to be displayed above the radioGroup.
- options – The text that describes what each radio button represents.

## radioGroupRatings

This type of input lets you specify a numbered scale to rate something:

```JSON
    {
      "type": "radioGroupRatings",
      "description": "radioGroupRatings",
      "minRatings": 1,
      "inStepsOf": 1,
      "numberOfRatings": 10
    }
```

![radioGroupRatings](/documentation/images/radioGroupRatings.png)

radioGroupRatings has 4 properties:

- description – This is the text displayed above the radio buttons.
- minRatings – This is the lowest rating that will be displayed.
- inStepsOf – This is how much each rating will be incremented by to get the next value.
- numberOfRatings – This is how many ratings will be shown.

<!-- ## checkbox

The checkbox specifies a single checkbox that can be checked.

```JSON
    {
      "type": "checkbox",
      "description": "Single checkbox"
    }
```

![singlecheckbox](/documentation/images/singlecheckbox.png)

The checkbox has one property – description. -->

## checkboxGroup

A checkboxGroup lets you have a list of checkboxes, any of which can be checked.

```JSON
    {
      "type": "checkboxGroup",
      "description": "Here is a checkboxGroup",
      "options": [
        "You can have ",
        "as many check boxes",
        "as you would like",
        "asdfasdfasdf"
      ]
    }
```

![checkBoxGroup](/documentation/images/checkBoxGroup.png)

There are 2 properties for a checkboxGroup:

- description – The text to be displayed above the checkboxes.
- options – The text that describes what each checkbox represents.

<!-- ## switch

The switch has two possible states.

```JSON
    {
      "type": "switch",
      "description": "here is a switch (not sure this will be useful)",
      "firstChoice": "first choice",
      "secondChoice": "second choice"
    }
```

![switch](/documentation/images/switch.png)

The switch has 3 properties:

- description – The prompt for the switch values.
- firstChoice – The first (default) option.
- secondChoice – The second option. -->

## dropDownList

Only one of the options in the dropDownList can be selected.

```JSON
    {
      "type": "dropDownList",
      "description": "Here is a dropDownList",
      "options": [
        "This will take up less page space",
        "more options",
        "even more options"
      ]
    }
```

![dropdown](/documentation/images/dropdown.png)

The dropdownList has 2 properties, which are the same as the checkboxGroup.

<!-- ## datePicker

The datePicker allows you to pick a date with an interactive calendar. This object has no additional properties.

```JSON
  {
    "type": "datePicker"
  }
```

<img src="/documentation/images/datepicker1.png" alt="datepicker1" width="200">

<img src="/documentation/images/datepicker2.png" alt="datepicker2" width="200">

## timePicker

The timePicker allows you to choose a time with an interactive menu. This object has no additional properties.

```JSON
  {
    "type": "timePicker"
  }
```

<img src="/documentation/images/timepicker1.png" alt="timepicker1" width="200">

<img src="/documentation/images/timepicker2.png" alt="timepicker2" width="200"> -->

## sectionBreak

The sectionBreak simply adds a horizontal rule to the form. This object has no additional properties.

```JSON
  {
    "type": "sectionBreak"
  }
```

![sectionBreak](/documentation/images/sectionBreak.png)
