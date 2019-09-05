DOCUMENT IS WORK IN PROGRESS!


# Getting started with JobTech Taxonomy API

Hello developer! This document will get you started using the API at http://jobtech-taxonomy-api.dev.services.jtech.se/v0/taxonomy/swagger-ui/index.html 
. The tools we provide gives you access to the Taxonomy Database containing terminology used in the Swedish labour market and the relationships between concepts within taxonomies like occupations, skills, education levels and much more!

The public API is open source (code found at https://github.com/JobtechSwe/jobtech-taxonomy-api) and the data is free to use by anyone. Make sure to read through the documentation for the specific resource you want, and the information about the Taxonomy Database, to understand what this API can offer. If you have any questions about the API or the data, don’t hesitate to contact us at contact@jobtechdev.se, or create an issue on Github if you find any bugs.

# Table of content
* [ Getting started - Short version ](#short)
* [ Getting started - Longer version ](#long)
  * [ Background - The Taxonomy Database ](#background)
    * [ Schema: Occupations ](#occupations)
    * [ Schema: Skills ](#skills)
    * [ Schema: Geographical places ](#geography)
    * [ Schema: Wage type ](#wageType)
    * [ Schema: Employment type ](#eType)
    * [ Schema: Driving licence ](#driving)
    * [ Schema: Worktime extent ](#worktime)
    * [ Schema: SNI ](#sni)
    * [ Schema: Languages ](#language)
    * [ Schema: Language levels ](#languageLevel)
    * [ Schema: Employment duration ](#employmentDuration)
  * [ Using the API ](#using)
    * [ Authentication ](#auth)
* [ Resources ](#resources)
    * [ Endpoint: /v0/taxonomy/public/versions ](#versions)
    * [ Endpoint: /v0/taxonomy/public/changes ](#changes)
    * [ Endpoint: /v0/taxonomy/public/concepts ](#concepts)
    * [ Endpoint: /v0/taxonomy/public/search ](#search)
    * [ Endpoint: /v0/taxonomy/public/replaced-by-changes ](#replaced)
    * [ Endpoint: /v0/taxonomy/public/concept/types ](#types)
    * [ Endpoint: /v0/taxonomy/public/parse-text ](#parse)
  * [ Results ](#results)
    * [ Successful queries ](#success)
    * [ Errors ](#error)
  * [ Use cases ](#useCases)
* [ Contact Information ](#contact)

<a name="short"></a>
# Getting started - Short version

Authorize with API key 111 and try it out! (Vi kommer att ändra detta till att begära ut en nyckel)

<a name="long"></a>
# Getting started - Longer version 

<a name="background"></a>
## Background - The Taxonomy Database 

The Taxonomy Database contains terms or phrases used within the Swedish labour market. These are called concepts in the database. Every concept in the database has a unique concept-ID, a preferred label and a type. Some concepts have extra attributes like definitions and alternative labels. The concepts are grouped together in schemas (see the schema headlines below) and within schemas the concepts from different types are linked with relationships.

The content of the Taxonomy Database is constantly improved and updated behind the scenes by Jobtech’s editorial team. When enough updates of the database has been made, a new version is released. However, nothing ever gets deleted in the Taxonomy Database. If a concept or an attribute becomes outdated it is tagged with a deprecated flag but it is still available in the API from some endpoints.

The Taxonomy Database contains a number of schemas. Some of these schemas are multilevel taxonomies with hierarchical relationships between concepts. Some schemas are merely simple collections of concepts. The following section will walk you through all the different schemas.

<a name="occupations"></a>
### Schema: Occupations


```
Chart created in www.draw.io
```

![alt text](https://github.com/JobtechSwe/jobtech-taxonomy-api/blob/develop/pictures-for-md/Untitled%20Diagram.png "Diagram for Occupation Schema")

The occupation taxonomy is a multilevel collection of occupations. The taxonomy is based on different external standards together with content created by the editorial team for use in the Swedish labour market, with the concepts all connected to each other directly or indirectly. Depending on your needs you might be interested in different parts of the schema. If you work with job seekers or employers the recommended types to use is the Occupation Field type together with SSYK-4 and Occupation Names. The Occupation collections and Keywords might also be useful. If you are working with official labour market statistics you are more likely to use the SSYK or ISCO types.

Some types in the Occupation schema comes from “Svensk standard för yrkesklassificering” (Swedish Standard Classification of Occupations), or SSYK. Another standard integrated in the schema is “International Standard Classification of Occupation”, or ISCO. The current version used is SSYK-2012 and ISCO-08. All the concepts in the SSYK and ISCO types have external-standard codes. If you are using the taxonomy for statistical reasons for example, these codes come in handy. Very important to note is that the SSYK and ISCO codes are not to be used as unique ID numbers for specific concepts since they are not fixed. When the external standard gets updated the SSYK and ISCO codes are moved around according to the new version of the standard. Always use the Concept ID as identification for specific concepts. This is guaranteed to not change over time. 

The external standard types at the topmost level in the schema (SSYK-1 and ISCO-1) contain general areas of work, like “Yrken med social inriktning”. Since the concepts at this level covers very broad areas of the labour market, there aren’t that many in each type. The third top level type, Occupation Field, is also very broad. This type isn’t an external standard but a collection of nonspecific occupation areas created specifically for the job seeking market. 

The lower you get in the taxonomy, the more detailed concepts you’ll find with Occupation Name at the bottom. This type contains more than 3000 concepts, collected by the editorial team often by suggestions from employers and industry organizations. In this level you’ll find concepts like “Stödpedagog”. 

Every concept at a lower and more detailed level is connected to one concept at the parent level, throughout the taxonomy. Example: 

```
Chart created in www.draw.io
```

![alt text](https://github.com/JobtechSwe/jobtech-taxonomy-api/blob/develop/pictures-for-md/Hierarchy.png "Diagram linked occupation levels")

In the type Occupation Collections you’ll find listings of Occupation Names grouped by different variables that may span over different occupation areas. examples are “Arbeten utan krav på utbildning” and “Chefsyrken”.
The Keyword type contains a variety of different search terms and phrases in some way related to Occupation Names. They can be used to help candidates find job ads they are interested in even if they don’t know the exact Occupation Name. An example is the Keyword “Coop” (like the food store), mapped to the Occupation Name “Butiksbiträde”. 

<a name="skills"></a>
### Schema: Skills

This taxonomy contains two levels. The top type contains a number of broader skill areas, like “Databaser”. The lower type contains specific skill concepts like “SQL-Base, databashanterare”. Each of these concepts are mapped to a parent skill headline in the above level. The database contains around 5500 skills as of May 2019.

<a name="geography"></a>
### Schema: Geographical places

The database contains a four level taxonomy of geographical places. Like the occupation taxonomy and the skill taxonomy the concepts are related to each other in a hierarchical structure.

The top geographic type lists all continents in the world, including Antarctica. The taxonomy is based on the UN standard. In this level you’ll also find the concept “Hela världen”, which can be used in cases where a system requires a location but a job seeker for example doesn’t want to specify.

The second type in this taxonomy contains all countries in the world. The countries are categorized according to ISO standard. Each country in this level has a parent continent in the top level.

The third type is simply called regions and it contains all regions within the EU with a “NUTS code” <CORRECT? Or is it all regions within Sweden and EU with OR without a NUTS code?> (See Eurostat for information about NUTS). In Sweden the regions corresponds to “län” <CORRECT?>. Every region is mapped to a specific parent country in the second level in the taxonomy. 

The fourth level in the geographic places taxonomy contains the Swedish municipalities. Each municipality is mapped to a specific parent region in the above level.

<a name="wageType"></a>
### Schema: Wage type

This schema only has one type. This type contains descriptions of different forms of payment, like “Rörlig ackords- eller provisionslön”.

<a name="eType"></a>
### Schema: Employment type

This schema only contain one type. It lists different types of employment, like “Sommarjobb / feriejobb” och “Behovsanställning”.

<a name="driving"></a>
### Schema: Driving licence

This single type schema contains all different driving licence categories in Sweden, according to EU standard, and the description and limitation of each licence. 

All but the “lowest” ranked licence also contain a list of the licences that are implicit within that level. The A2 licence for example has the Implicit licence attribute listing AM and A1. These are lower level licences for scooters that you are automatically allowed to drive if you carry the A2 licence.

<a name="worktime"></a>
### Schema: Worktime extent
 
This schema only contains the two concepts “Heltid” and “Deltid”.
(DEN NYA SUN ÄR FÖRHOPPNINGSVIS FÄRDIG OM NÅGRA VECKOR men rimligt löfte är i september. Rita säger: skippa nuvarande versionen och tryck in den nya när den kommer ut)

<a name="sni"></a>
### Schema: SNI

SNI stands for “svensk näringsgrensindelning” and the collection contains terms for different industries. This taxonomy follows the SCB documentation and has two levels. 

The SNI-level-1 contains general area term of industries. An example is the concept “Tillverkning”.

The second level, SNI-level-2, lists the industries in more detail. It has concepts like “Livsmedelsframställning”. Every concept in this level has a parent concept in the first level.

<a name="language"></a>
### Schema: Languages

The language taxonomy lists more than 400 natural languages in the world, like “Svenska” and “Xhosa/Isixhosa”. The language names follows the ISO standard.

<a name="languageLevel"></a>
### Schema: Language levels

The language level taxonomy is a simple collection of different terms used to describe language proficiency. It contains concepts like “Lite” and “Flytande”.

<a name="employmentDuration"></a>
### Schema: Employment duration

The employment duration taxonomy contains concepts describing how long an employment is meant to last. The schema contains concepts like “3 månader – upp till 6 månader”.

<a name="using"></a>
## Using the API

<a name="auth"></a>
### Authentication

At http://jobtech-taxonomy-api-develop.dev.services.jtech.se/v0/taxonomy/swagger-ui/index.html#/ <CORRECT LINK?> you’ll see the headlines public and private. You can access the endpoints under public by authenticating with the API key 111 <Explain the procedure for getting a proper API key>. The private endpoint is only for the editorial team and not open to the public.

<a name="resources"></a>
## Resources

<a name="versions"></a>
### Endpoint: /v0/taxonomy/public/versions

The response is a list of all versions of the database that exists. It starts with version 0 <IS THIS CORRECT?>  - which is the empty database - and ends with the last published version.

<a name="changes"></a>
### Endpoint: /v0/taxonomy/public/changes

This endpoint returns a list of all “events” that has occured for concepts in the database between versions. The possible events are “CREATED”, “DEPRECATED” and “UPDATED”. The earliest possible version that exists is 0 <IS THIS CORRECT?>, which represent the database before any data were created in it. If you choose to see the changes from version 0 to version 1 you will see all the concepts that were added to the database in the first iteration. If you don’t choose any specific “toVersion” parameter you will get changes that happened all the way to the latest version by default.

<a name="concepts"></a>
### Endpoint: /v0/taxonomy/public/concepts

This endpoint allows you to search specific concepts in the database. In the response you’ll see the concept ID, the description and the type. If the concept is outdated (i.e. no longer recommended for use in the Swedish labour market) you’ll also see  “deprecated”: true in the json response.

<a name="search"></a>
### Endpoint: /v0/taxonomy/public/search

This endpoint lets you search concepts based on part of the label. The result will contain all concepts with the search string, no matter if the search string is at the beginning, middle or end of the concept label. Like the concepts endpoint, the result will contain the concept ID, the description, the type and if applicable also the “deprecated”: true attribute.

<a name="replaced"></a>
### Endpoint: /v0/taxonomy/public/replaced-by-changes

Similar to the Changes endpoint, this will give you changes between versions of the database but in this endpoint you will only see concepts that has been replaced by another concept. This means one concept has been deprecated and is pointing to another existing concept in the database. One example is the Occupation Name “Användbarhetsexpert” with Concept ID HciA_Cu7_FXt from version 1, that has been replaced by “Interaktionsdesigner” with Concept ID QQjZ_NAN_bDR in version 2. The replaced concept will still be searchable in the database but from this point it will have the Deprecated-flag attached to it.

<a name="types"></a>
### Endpoint: /v0/taxonomy/public/concept/types

With this endpoint you’ll get a list with all types that exists in the taxonomy.

<a name="parse"></a>
### Endpoint: /v0/taxonomy/public/parse-text

The Parse Text is a “post” endpoint, allowing you to input a string. The response will return all the concepts from the database that relates to the words in the input text. For example, if you input the string “I Skövde arbetade jag som clown. Jag är även bra på handkirurgi.”, the result will contain the concepts “Skövde” (Municipality), “clown” (Occupation name) and “handkirurgi” (Skill).

This endpoint can be used to automatically parse CVs to match words or phrases to job ads. Or vice versa. 

<a name="results"></a>
### Results

All API responses are in json format.

<a name="success"></a>
### Successful queries

Successful requests will return the HTTP status code 200. The details in the response will vary depending on the specific endpoint. 

<a name="error"></a>
### Errors

The three possible error codes are listed below. Note that a request returning an empty list does not count as an error and will return status code 200.

HTTP Status code
Reason
Explanation
500
Internal Server Error
Something wrong on the server side
400
Bad Request
Something wrong in the query
401
Unauthorized
You are not using an API key

<a name="useCases"></a>
## Use cases

Link to Ulf’s example apllication

<a name="contact"></a>
# Contact Information


Bug reports are issued at the Github repo.
Questions about the Taxonomy database, about Jobtech, about the API in general are best emailed to contact@jobtechdev.se.
Check out more open API:s at jobtechdev.se
